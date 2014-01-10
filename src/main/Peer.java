package main;

import inode.Inode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.util.IOUtils;

import net.jxta.discovery.DiscoveryEvent;
import net.jxta.discovery.DiscoveryListener;
import net.jxta.discovery.DiscoveryService;
import net.jxta.document.AdvertisementFactory;
import net.jxta.endpoint.ByteArrayMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.Message.ElementIterator;
import net.jxta.endpoint.MessageElement;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeMsgEvent;
import net.jxta.pipe.PipeMsgListener;
import net.jxta.pipe.PipeService;
import net.jxta.platform.Module;
import net.jxta.platform.ModuleClassID;
import net.jxta.platform.NetworkConfigurator;
import net.jxta.platform.NetworkManager;
import net.jxta.protocol.ModuleClassAdvertisement;
import net.jxta.protocol.ModuleImplAdvertisement;
import net.jxta.protocol.ModuleSpecAdvertisement;
import net.jxta.protocol.PipeAdvertisement;
import fileUpload.FileUpload;

public class Peer implements DiscoveryListener, PipeMsgListener {

	// handle these exceptions
	public static void main(String[] args) throws PeerGroupException,
			IOException {

		// JXTA logs a lot, you can configure it setting level here
		Logger.getLogger("net.jxta").setLevel(Level.SEVERE);
		// Logger.getLogger("net.jxta").setLevel(Level.ALL);

		int port = 9000 + new Random().nextInt(100);

		Peer hello = new Peer(port);
		hello.start();
		hello.fetch_advertisements();
	}

	private String peer_name;
	private PeerID peer_id;
	private File conf;
	private NetworkManager manager;

	public Peer(int port) {
		System.out.println("Port used: " + port);
		// Add a random number to make it easier to identify by name, will also
		// make sure the ID is unique
		peer_name = "Peer " + new Random().nextInt(1000000);
		System.out.println("Peer Name: " + peer_name);

		// This is what you will be looking for in Wireshark instead of an IP,
		// hint: filter by "jxta"
		peer_id = IDFactory.newPeerID(PeerGroupID.defaultNetPeerGroupID,
				peer_name.getBytes());

		// Here the local peer cache will be saved, if you have multiple peers
		// this must be unique
		conf = new File("." + System.getProperty("file.separator") + peer_name);

		// Most documentation you will find use a deprecated network manager
		// setup, use this one instead
		// ADHOC is usually a good starting point, other alternatives include
		// Edge and Rendezvous
		try {
			manager = new NetworkManager(NetworkManager.ConfigMode.ADHOC,
					peer_name, conf.toURI());
		} catch (IOException e) {
			// Will be thrown if you specify an invalid directory in conf
			e.printStackTrace();
		}

		NetworkConfigurator configurator;
		try {
			// Settings Configuration
			configurator = manager.getConfigurator();
			configurator.setTcpPort(port);
			configurator.setTcpEnabled(true);
			configurator.setTcpIncoming(true);
			configurator.setTcpOutgoing(true);
			configurator.setUseMulticast(true);
			configurator.setPeerID(peer_id);
		} catch (IOException e) {
			// Never caught this one but let me know if you do =)
			e.printStackTrace();
		}
	}

	private static final String subgroup_name = "Make sure this is spelled the same everywhere";
	private static final String subgroup_desc = "...";
	private static final PeerGroupID subgroup_id = IDFactory.newPeerGroupID(
			PeerGroupID.defaultNetPeerGroupID, subgroup_name.getBytes());

	private static final String unicast_name = "This must be spelled the same too";
	private static final String multicast_name = "Or else you will get the wrong PipeID";

	private static final String service_name = "And dont forget it like i did a million times";

	private PeerGroup subgroup;
	private PipeService pipe_service;
	private PipeID unicast_id;
	private PipeID multicast_id;
	private PipeID service_id;
	private DiscoveryService discovery;
	private ModuleSpecAdvertisement mdadv;

	public void start() throws PeerGroupException, IOException {
		// Launch the missiles, if you have logging on and see no exceptions
		// after this is ran, then you probably have at least the jars setup
		// correctly.
		PeerGroup net_group = manager.startNetwork();

		// Connect to our subgroup (all groups are subgroups of Netgroup)
		// If the group does not exist, it will be automatically created
		// Note this is suggested deprecated, not sure what the better way is
		ModuleImplAdvertisement mAdv = null;
		try {
			mAdv = net_group.getAllPurposePeerGroupImplAdvertisement();
		} catch (Exception ex) {
			System.err.println(ex.toString());
		}
		subgroup = net_group.newGroup(subgroup_id, mAdv, subgroup_name,
				subgroup_desc);

		// A simple check to see if connecting to the group worked
		if (Module.START_OK != subgroup.startApp(new String[0]))
			System.err.println("Cannot start child peergroup");

		// We will spice things up to a more interesting level by sending
		// unicast and multicast messages
		// In order to be able to do that we will create to listeners that will
		// listen for
		// unicast and multicast advertisements respectively. All messages will
		// be handled by Hello in the
		// pipeMsgEvent method.

		unicast_id = IDFactory.newPipeID(subgroup.getPeerGroupID(),
				unicast_name.getBytes());
		multicast_id = IDFactory.newPipeID(subgroup.getPeerGroupID(),
				multicast_name.getBytes());

		pipe_service = subgroup.getPipeService();
		pipe_service
				.createInputPipe(get_advertisement(unicast_id, false), this);
		pipe_service.createInputPipe(get_advertisement(multicast_id, true),
				this);

		// In order to for other peers to find this one (and say hello) we will
		// advertise a Hello Service.
		discovery = subgroup.getDiscoveryService();
		discovery.addDiscoveryListener(this);

		ModuleClassAdvertisement mcadv = (ModuleClassAdvertisement) AdvertisementFactory
				.newAdvertisement(ModuleClassAdvertisement
						.getAdvertisementType());

		mcadv.setName("STACK-OVERFLOW:HELLO");
		mcadv.setDescription("Tutorial example to use JXTA module advertisement Framework");

		ModuleClassID mcID = IDFactory.newModuleClassID();

		mcadv.setModuleClassID(mcID);

		// Let the group know of this service "module" / collection
		discovery.publish(mcadv);
		discovery.remotePublish(mcadv);

		mdadv = (ModuleSpecAdvertisement) AdvertisementFactory
				.newAdvertisement(ModuleSpecAdvertisement
						.getAdvertisementType());
		mdadv.setName("STACK-OVERFLOW:HELLO");
		mdadv.setVersion("Version 1.0");
		mdadv.setCreator("sun.com");
		mdadv.setModuleSpecID(IDFactory.newModuleSpecID(mcID));
		mdadv.setSpecURI("http://www.jxta.org/Ex1");

		service_id = IDFactory.newPipeID(subgroup.getPeerGroupID(),
				service_name.getBytes());
		PipeAdvertisement pipeadv = get_advertisement(service_id, false);
		mdadv.setPipeAdvertisement(pipeadv);

		// Let the group know of the service
		discovery.publish(mdadv);
		discovery.remotePublish(mdadv);

		// Start listening for discovery events, received by the discoveryEvent
		// method
		pipe_service.createInputPipe(pipeadv, this);

		new Thread("send messages thread") {
			public void run() {
				while (true) {
					// System.out.println("enter your message: ");
					// String msgg = user_input.next();
					// send_to_peer(msgg, peer_ids);
					for (String s : peer_ids) {
						send_to_peer("Hello", s);
					}
					try {
						Thread.sleep(4000);
					} catch (InterruptedException e) {
					}
				}
			}
		}.start();
	}

	Scanner user_input = new Scanner(System.in);

	private void UploadFile() throws IOException {
		System.out.println("Enter filename to be uploaded: ");
		String file = user_input.next();
		System.out.println("Enter path: ");
		String filepath = user_input.next();

		// copy to local machine
		File source = new File(file);
		File dest = new File("Upload/Files" + filepath + file);
		InputStream is = null;
		OutputStream os = null;
		try {
			is = new FileInputStream(source);
			os = new FileOutputStream(dest);
			IOUtils.copy(is, os);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("File not found");
		} finally {
			is.close();
			os.close();
		}

		String peer_id_str = peer_ids.get(0);
		new FileUpload(pipe_service, unicast_id, file, filepath, peer_id_str);
	}

	public static PipeAdvertisement get_advertisement(PipeID id,
			boolean is_multicast) {
		PipeAdvertisement adv = (PipeAdvertisement) AdvertisementFactory
				.newAdvertisement(PipeAdvertisement.getAdvertisementType());
		adv.setPipeID(id);
		if (is_multicast)
			adv.setType(PipeService.PropagateType);
		else
			adv.setType(PipeService.UnicastType);
		adv.setName("This however");
		adv.setDescription("does not really matter");
		return adv;
	}

	private List<String> peer_ids = new ArrayList<String>();

	@Override
	public void discoveryEvent(DiscoveryEvent event) {
		// Found another peer! Let's say hello shall we!
		// Reformatting to create a real peer id string
		String found_peer_id = "urn:jxta:"
				+ event.getSource().toString().substring(7);
		if (!peer_ids.contains(found_peer_id)) {
			peer_ids.add(found_peer_id);
		}
		System.out.println(peer_ids);
		// System.out.println("enter your message: ");
		// String msgg = user_input.next();
		// send_to_peer("Hello", found_peer_id);
	}

	private void send_to_peer(String message, String found_peer_id) {
		// This is where having the same ID is important or else we wont be
		// able to open a pipe and send messages
		PipeAdvertisement adv = get_advertisement(unicast_id, false);

		// Send message to all peers in "ps", just one in our case
		Set<PeerID> ps = new HashSet<PeerID>();
		try {
			ps.add((PeerID) IDFactory.fromURI(new URI(found_peer_id)));
		} catch (URISyntaxException e) {
			// The JXTA peer ids need to be formatted as proper urns
			e.printStackTrace();
		}

		// A pipe we can use to send messages with
		OutputPipe sender = null;
		System.out.println(ps);
		try {
			sender = pipe_service.createOutputPipe(adv, ps, 10000);
		} catch (IOException e) {
			// Thrown if there was an error opening the connection, check
			// firewall settings
			e.printStackTrace();
		}

		Message msg = new Message();
		MessageElement fromElem = null;
		MessageElement msgElem = null;
		try {
			fromElem = new ByteArrayMessageElement("From", null, peer_id
					.toString().getBytes("ISO-8859-1"), null);
			msgElem = new ByteArrayMessageElement("Msg", null,
					message.getBytes("ISO-8859-1"), null);
		} catch (UnsupportedEncodingException e) {
			// Yepp, you want to spell ISO-8859-1 correctly
			e.printStackTrace();
		}

		msg.addMessageElement(new StringMessageElement("CHECK", "Msg", null));
		msg.addMessageElement(fromElem);
		msg.addMessageElement(msgElem);

		try {
			sender.send(msg);
		} catch (IOException e) {
			// Check, firewall, settings.
			e.printStackTrace();
		}
	}

	private void updateInode(String filepath, String filename)
			throws IOException {

		Inode inode = new Inode(pipe_service, unicast_id);
		inode.addFile(filepath + filename);
		inode.sendInode(peer_ids.get(0));

	}

	@Override
	public void pipeMsgEvent(PipeMsgEvent event) {
		// Someone is sending us a message!
		try {
			Message msg = event.getMessage();
			/*
			 * ElementIterator it = msg.getMessageElements(); while
			 * (it.hasNext()) { MessageElement el = it.next();
			 * System.out.println("Element : " + it.getNamespace() + " :: " +
			 * el.getElementName()); System.out.println("[" + el + "]"); }
			 */

			try {
				// check CHECK msg element for File or Msg
				String check = msg.getMessageElement("CHECK").toString();

				if (check.equalsIgnoreCase("File")) {
					System.out.println("File received.");
					String filename = msg.getMessageElement("filename")
							.toString();
					String filepath = msg.getMessageElement("filepath")
							.toString();
					ElementIterator it = msg.getMessageElements();
					MessageElement el = null;
					while (it.hasNext()) {
						el = it.next();
					}
					MessageElement file = el;
					if (file.getElementName() != null) {
						try {
							File tempFile = new File("Upload/Files" + filepath);
							if (tempFile.mkdirs()) {
								System.out
										.println("New directory path created");
							} else {
								System.out.println("Directory creation failed");
							}
							FileOutputStream out = new FileOutputStream(
									new File("Upload/Files" + filepath
											+ filename));

							file.sendToStream(out);

							updateInode(filepath, filename);
						} catch (IOException err) {
							System.out.println(err);
						}
						System.out.println("File download complete!!");
					}
				} else if (check.equalsIgnoreCase("InodeFile")) {
					System.out.println("Inode File received.");
					ElementIterator it = msg.getMessageElements();
					MessageElement el = null;
					while (it.hasNext()) {
						el = it.next();
					}
					MessageElement file = el;
					if (file.getElementName() != null) {
						try {
							FileOutputStream out = new FileOutputStream(
									new File("inode"));
							file.sendToStream(out);
						} catch (IOException err) {
							System.out.println(err);
						}
						System.out.println("File download complete!!");
					}
				} else if (check.equalsIgnoreCase("Msg")) {
					byte[] msgBytes = msg.getMessageElement("Msg").getBytes(
							true);
					byte[] fromBytes = msg.getMessageElement("From").getBytes(
							true);
					String from = new String(fromBytes);
					String message = new String(msgBytes);
					System.out.println(message + " says " + from);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			// You will notice that JXTA is not very specific with exceptions...
			e.printStackTrace();
		}
	}

	/**
	 * We will not find anyone if we are not regularly looking
	 */
	private void fetch_advertisements() {
		new Thread("fetch advertisements thread") {
			public void run() {
				while (true) {
					discovery.getRemoteAdvertisements(null,
							DiscoveryService.ADV, "Name",
							"STACK-OVERFLOW:HELLO", 1, null);
					try {
						sleep(10000);

					} catch (InterruptedException e) {
					}
				}
			}
		}.start();
	}
}