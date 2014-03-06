package inode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import main.Hashing;
import main.Peer;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.exception.PeerGroupException;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;
import fileUpload.FileUpload;

public class Inode {

	public final String rootPrefix = "Upload/Files";

	private PipeService pipe_service;
	private PipeID unicast_id;

	public Inode() throws IOException {
		this.pipe_service = Peer.getPipe_service();
		this.unicast_id = Peer.getUnicast_id();
	}

	public String readFromInode(String inodeFile) throws IOException {
		StringBuilder list = new StringBuilder();
		String text = null;
		try {
			BufferedReader reader = new BufferedReader(
					new FileReader(inodeFile));
			String line = null;
			while ((line = reader.readLine()) != null) {
				list.append(line + "\n");
			}
			reader.close();
			text = list.toString();
		} catch (FileNotFoundException e) {
			// TODO Request inode file and then execute the function again
			text = "Error: inode file not found in " + inodeFile;
			e.printStackTrace();
		}
		return text;
	}

	// APPENDS a file path to an inode file
	public void writeToInode(String inodeFile, String path) throws IOException {
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(
				inodeFile, true)));
		writer.println(path);
		writer.close();
	}

	public void removeFromInode(String inodeFile, String path)
			throws IOException {
		List<String> DirInodes = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader(inodeFile));
		String line = br.readLine();
		while (line != null) {
			DirInodes.add(line);
			line = br.readLine();
		}
		br.close();

		// Now write the DirInodes String list to the inode file
		PrintWriter writer = null;
		writer = new PrintWriter(inodeFile);

		for (String s : DirInodes) {
			if (s.equals(path)) {
				// Don't add this entry in the inode file
				continue;
			}
			writer.println(s);
		}
		writer.close();
	}

	public boolean searchFile(String inodeFile, String filename)
			throws IOException {
		boolean present = false;
		List<String> DirInodes = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new FileReader(inodeFile));
		String line = br.readLine();
		while (line != null) {
			DirInodes.add(line);
			line = br.readLine();
		}
		br.close();

		for (String s : DirInodes) {
			if (s.equals(filename)) {
				present = true;
				break;
			}
		}

		return present;
	}

	// DO NOT include "Upload/Files" in `inodePath`
	public void sendInode(String inodePath) {
		List<String> peer_ids = Peer.getPeer_ids();

		// calculate peer id to be sent to using the hashing function
		String filehash1 = Hashing.sha1(inodePath + "1");
		// String filehash2 = Hashing.sha1(inodePath + "2");
		// String filehash3 = Hashing.sha1(inodePath + "3");
		// String filehash4 = Hashing.sha1(inodePath + "4");
		// String filehash5 = Hashing.sha1(inodePath + "5");
		String peer_id_str1 = Hashing.bestMatch(peer_ids, filehash1);
		// String peer_id_str2=Hashing.bestMatch(peer_ids, filehash2);
		// String peer_id_str3=Hashing.bestMatch(peer_ids, filehash3);
		// String peer_id_str4=Hashing.bestMatch(peer_ids, filehash4);
		// String peer_id_str5=Hashing.bestMatch(peer_ids, filehash5);

		System.out.println("Sending inode file to " + peer_id_str1);
		int t = inodePath.lastIndexOf("/");
		String filepath = inodePath.substring(0, t + 1);
		new FileUpload("inode", filepath, filepath, peer_id_str1);
	}

	// DO NOT include "Upload/Files" in `inodePath`
	public void requestInode(String inodePath) {
		List<String> peer_ids = Peer.getPeer_ids();

		// calculate peer id to download the file from
		String filehash = Hashing.sha1(inodePath + "1");
		String peer_id = Hashing.bestMatch(peer_ids, filehash);

		int t = inodePath.lastIndexOf("/");
		String iFilepath = inodePath.substring(0, t + 1);
		Message MyMessage = new Message();
		MyMessage.addMessageElement(new StringMessageElement("CHECK",
				"Download", null));
		MyMessage.addMessageElement(new StringMessageElement("filename",
				"inode", null));
		MyMessage.addMessageElement(new StringMessageElement("filepath",
				iFilepath, null));
		MyMessage.addMessageElement(new StringMessageElement("peer", Peer
				.getPeer_id().toString(), null));

		PipeAdvertisement adv = Peer.get_advertisement(unicast_id, false);

		Set<PeerID> ps = new HashSet<PeerID>();
		try {
			ps.add((PeerID) IDFactory.fromURI(new URI(peer_id)));
		} catch (URISyntaxException e) {
			// The JXTA peer ids need to be formatted as proper urns
			e.printStackTrace();
		}

		// A pipe we can use to send messages with
		OutputPipe sender = null;
		System.out.println("Sending inode file download request to " + ps);
		try {
			sender = pipe_service.createOutputPipe(adv, ps, 10000);
		} catch (IOException e) {
			// Thrown if there was an error opening the connection,
			// check firewall settings
			System.out.println("Error opening connection");
			e.printStackTrace();
		}

		try {
			sender.send(MyMessage);
		} catch (IOException e) {
			// Check, firewall, settings.
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException,
			PeerGroupException, InterruptedException {
		int port = 9000 + new Random().nextInt(100);
		Peer hello = new Peer(port);
		hello.start();
		hello.fetch_advertisements();
		Thread.sleep(5000);

		Inode i = new Inode();
		// System.out.println(i.readFromInode("Upload/Files/acads/gopi/inode"));
		i.requestInode("/acads/gopi/inode");
		// System.out.println(i.searchFile("Upload/Files/acads/gopi/inode",
		// "Upload/Files/acads/gopi/test/"));
		// i.writeToInode("Upload/Files/acads/gopi/inode",
		// "Upload/Files/acads/gopi/1");
		// System.out.println(i.readFromInode("Upload/Files/acads/gopi/inode"));
		// i.removeFromInode("Upload/Files/acads/gopi/inode",
		// "Upload/Files/acads/gopi/1");
		// System.out.println(i.readFromInode("Upload/Files/acads/gopi/inode"));
	}
}
