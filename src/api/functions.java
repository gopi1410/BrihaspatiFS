package api;

import inode.Inode;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Peer;
import net.jxta.exception.PeerGroupException;

public class functions {

	Peer hello;

	public functions() throws PeerGroupException, IOException,
			InterruptedException {
		int port = 9000 + new Random().nextInt(100);
		hello = new Peer(port);
		hello.start();
		hello.fetch_advertisements();
		Thread.sleep(5000);
	}

	private String PWD;

	public String getPWD() {
		return PWD;
	}

	public void setPWD(String pWD) {
		this.PWD = pWD;
	}

	private static boolean isValidName(String text) {
		Pattern pattern = Pattern.compile("([a-zA-Z_0-9]+)/?",
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(text);
		boolean isMatch = matcher.matches();
		return isMatch;
	}

	public String ls() throws IOException, InterruptedException {
		String pwd = getPWD();
		String inodePath = "Upload/Files" + pwd + "inode";

		// Request the inode file first
		Peer.setFileReceivedCheck(false);
		Inode i = new Inode();
		i.requestInode(pwd + "inode");

		// Call ls on inode file after inode file is received
		Object o = new Object();
		synchronized (o) {
			Peer.getFileReceivedCheck();
		}
		Thread.sleep(500); // Required if inode file is missing.

		// fileCheck = Peer.getFileReceivedCheck();
		// System.out.println(fileCheck);

		// System.out.println(fileCheck);
		// while (fileCheck == false) {
		// Thread.sleep(10);
		// fileCheck = Peer.getFileReceivedCheck();
		// }
		// System.out.println(fileCheck);

		return i.readFromInode(inodePath);
	}

	public void cd(String path) {
		// TODO Inode operations

		String pwd = getPWD();
		String newPath = null;
		if (path.charAt(0) == '/') {
			// it is absolute path, check if exists and change PWD
			newPath = path;
		} else {
			// it is relative path
			// add it to PWD and check if the path exists
			newPath = pwd + path;
		}

		File f = new File("Upload/Files" + newPath);
		if (!f.exists()) {
			// directory does not exist
			System.out.println("No such file or directory");
		} else if (!f.isDirectory()) {
			// it is a file; can't cd to a file
			System.out.println("Not a directory");
		} else {
			// directory exists! cd to it
			if (newPath.charAt(newPath.length() - 1) != '/') {
				newPath = newPath + '/';
			}
			setPWD(newPath);
		}
	}

	public void mkdir(String dirname) throws IOException {
		if (!isValidName(dirname)) {
			System.out.println("Invalid directory name");
			return;
		} else {
			String pwd = getPWD();
			String newPath = pwd + dirname;
			if (newPath.charAt(newPath.length() - 1) != '/') {
				newPath = newPath + '/';
			}

			// Create directory
			File f = new File("Upload/Files" + newPath);
			if (f.exists()) {
				System.out.println("Directory '" + dirname + "' exists");
			} else {
				boolean result = f.mkdir();
				if (!result) {
					System.out.println("Directory creation failed");
					return;
				}
			}

			// Create inode in new directory
			File tempFile = new File("Upload/Files" + newPath + "inode");
			tempFile.createNewFile();

			// Update inode of parent directory
			Inode i = new Inode();
			i.writeToInode("Upload/Files" + pwd + "inode", "Upload/Files"
					+ newPath);

			// Send inode to its peer
			i.sendInode(pwd + "inode");
		}
	}

	public void rm(String file) throws IOException, InterruptedException {
		String pwd = getPWD();

		// TODO Check for absolute file path
		// OR sub directories as/test.test/txt

		// get inode of parent folder
		String inodePath = "Upload/Files" + pwd + "inode";
		String filePath = "Upload/Files" + pwd + file;
		Peer.setFileReceivedCheck(false);
		Inode i = new Inode();
		i.requestInode(pwd + "inode");

		// Further operations after inode file is received
		Object o = new Object();
		synchronized (o) {
			Peer.getFileReceivedCheck();
		}
		Thread.sleep(500); // Required if inode file is missing.

		// Check if file present or not
		boolean fileCheck = i.searchFile(inodePath, filePath);
		if (fileCheck == false) {
			System.out.println("No such file");
			return;
		}

		// If yes, delete the inode entry; else error deleting
		i.removeFromInode(inodePath, filePath);

		// delete file locally
		File f = new File(filePath);
		if (f.delete()) {
			System.out.println("File deleted successfully");
		} else {
			System.out.println("Delete operation failed");
		}

		// Send inode to its peer
		i.sendInode(pwd + "inode");
	}

	public void ltor(String fname, String fpath) throws IOException {
		hello.UploadFile(fname, fpath, null, null);
	}

	public void rtol(String fname, String fpath) {
		hello.RequestFile(fname, fpath);
	}

	public static void main(String[] args) throws IOException,
			PeerGroupException, InterruptedException {

		functions f = new functions();
		f.setPWD("/acads/gopi/");
		System.out.println(f.ls());
		// f.cd("check");
		// System.out.println(f.getPWD());
		// f.rm("1.txt");
		// System.out.println(f.ls());
		// f.mkdir("testing/");
	}

}
