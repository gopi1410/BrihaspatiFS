package inode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.Peer;
import net.jxta.document.MimeMediaType;
import net.jxta.endpoint.InputStreamMessageElement;
import net.jxta.endpoint.Message;
import net.jxta.endpoint.StringMessageElement;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.pipe.OutputPipe;
import net.jxta.pipe.PipeID;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

public class Inode {

	public final String rootPrefix = "Upload/Files";
	private List<String> DirInodes = new ArrayList<String>();

	private PipeService pipe_service;
	private PipeID unicast_id;

	public Inode(PipeService pipe_service, PipeID unicast_id)
			throws IOException {
		this.pipe_service = pipe_service;
		this.unicast_id = unicast_id;
		readFromFile();
	}

	public void writeToFile() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter("inode", "UTF-8");
		} catch (FileNotFoundException e) {
			// e.printStackTrace();
			System.out.println("Inode file not found");
		} catch (UnsupportedEncodingException e) {
			System.out.println("Unsupported inode file encoding");
			e.printStackTrace();
		}
		for (String s : DirInodes) {
			writer.println(s);
		}
		writer.close();
	}

	public void readFromFile() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("inode"));
		try {
			String line = br.readLine();
			while (line != null) {
				DirInodes.add(line);
				line = br.readLine();
			}
		} finally {
			br.close();
		}
	}

	public boolean searchFile(String filename) throws IOException {
		boolean present = false;
		readFromFile();

		for (String s : DirInodes) {
			if (s.endsWith(filename)) {
				present = true;
				break;
			}
		}

		return present;
	}

	public void addFile(String pathname) throws IOException {
		// readFromFile();
		if (!DirInodes.contains(rootPrefix + pathname)) {
			DirInodes.add(rootPrefix + pathname);
		}
		writeToFile();
	}

	public void sendInode(String peer_id) {

		FileInputStream in;
		InputStreamMessageElement MyInputStreamMessageElement;

		try {
			in = new FileInputStream(new File("inode"));
			MyInputStreamMessageElement = new InputStreamMessageElement("FILE",
					MimeMediaType.TEXT_DEFAULTENCODING, in, null);
			Message MyMessage = new Message();
			MyMessage.addMessageElement(new StringMessageElement("CHECK",
					"InodeFile", null));
			MyMessage.addMessageElement("FileSend",
					MyInputStreamMessageElement, null);

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
			System.out.println("Sending file to " + ps);
			try {
				sender = pipe_service.createOutputPipe(adv, ps, 10000);
			} catch (IOException e) {
				// Thrown if there was an error opening the connection,
				// check
				// firewall settings
				System.out.println("Error opening connection");
				e.printStackTrace();
			}

			try {
				sender.send(MyMessage);
			} catch (IOException e) {
				// Check, firewall, settings.
				e.printStackTrace();
			}

		} catch (FileNotFoundException e) {
			System.out.println("inode file not found!");
		} catch (SecurityException e) {
			System.out.println("security exception");
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	/*
	 * public static void main(String[] args) throws IOException { Inode inode =
	 * new Inode(); inode.addFile("1.txt"); inode.addFile("1.py");
	 * inode.addFile("acads/ee627/readme"); }
	 */
}