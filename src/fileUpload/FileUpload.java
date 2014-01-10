package fileUpload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
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

public class FileUpload {

	private static PipeService pipe_service;
	private static PipeID unicast_id;

	public FileUpload(PipeService pipe_service, PipeID unicast_id,
			String filename, String filepath, String peer_id) {
		FileUpload.pipe_service = pipe_service;
		FileUpload.unicast_id = unicast_id;

		Thread thread = new Thread(new UploadHandler(filename, filepath,
				peer_id), "file upload thread");
		thread.start();
	}

	private static class UploadHandler implements Runnable {

		private String filename;
		private String filepath;
		private String peer_id;

		UploadHandler(String filename, String filepath, String peer_id) {
			this.filename = filename;
			this.filepath = filepath;
			this.peer_id = peer_id;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			uploadDocument(filename, peer_id);
		}

		public void uploadDocument(String filename, String peer_id) {
			System.out.println("Uploading file " + filename + " to peer "
					+ peer_id);

			FileInputStream in;
			InputStreamMessageElement MyInputStreamMessageElement;

			try {
				in = new FileInputStream(new File(filename));
				MyInputStreamMessageElement = new InputStreamMessageElement(
						"FILE", MimeMediaType.TEXT_DEFAULTENCODING, in, null);
				Message MyMessage = new Message();
				MyMessage.addMessageElement(new StringMessageElement("CHECK",
						"File", null));
				MyMessage.addMessageElement(new StringMessageElement(
						"filename", filename, null));
				MyMessage.addMessageElement(new StringMessageElement(
						"filepath", filepath, null));
				MyMessage.addMessageElement("FileSend",
						MyInputStreamMessageElement, null);

				PipeAdvertisement adv = Peer.get_advertisement(unicast_id,
						false);

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
				System.out.println("file not found!" + filename);
			} catch (SecurityException e) {
				System.out.println("security exception");
			} catch (IOException e) {
				System.out.println(e);
			}
			return;

		}

	}
}
