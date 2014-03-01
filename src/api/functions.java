package api;

import inode.Inode;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class functions {

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

	public String ls() throws IOException {
		String pwd = getPWD();
		String inodePath = "Upload/Files" + pwd + "inode";

		// TODO Request the inode file first

		Inode i = new Inode();
		return i.readFromInode(inodePath);
	}

	public void cd(String path) {
		// TODO Inode operations
		// TODO handle .. (if necessary)

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
			// TODO ^this
		}
	}

	public void rm(String file) {
		
	}

	public static void main(String[] args) throws IOException {
		functions f = new functions();
		f.setPWD("/acads/gopi/");
		System.out.println(f.ls());
		f.cd("check");
		System.out.println(f.getPWD());
		System.out.println(f.ls());
		// f.mkdir("testing/");
	}

}
