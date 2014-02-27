package main;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Hashing {

	// public static void main(String[] args) throws NoSuchAlgorithmException {
	// // System.out.println(sha1("test string to sha1"));
	// List<String> singleAddress = new ArrayList<String>();
	// singleAddress.add("17 Fake Street");
	// singleAddress.add("Phoney town");
	// singleAddress.add("Makebelieveland");
	// System.out.println("Search: " + sha1("lol"));
	// System.out.println(sha1("17 Fake Street") + "\n" + sha1("Phoney town")
	// + "\n" + sha1("Makebelieveland"));
	// System.out.println(bestMatch(singleAddress, sha1("lol")));
	// }

	public static String sha1(String input) throws NoSuchAlgorithmException {
		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		byte[] result = mDigest.digest(input.getBytes());
		StringBuffer hash = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			hash.append(Integer.toString((result[i] & 0xff) + 0x100, 16)
					.substring(1));
		}

		return hash.toString();
		// return sb.length() + "x";
	}

	public static String bestMatch(List<String> peer_ids, String hash)
			throws NoSuchAlgorithmException {
		String best = null;
		int min = 10, idx = 0;
		int size = peer_ids.size();
		boolean flag = false;

		// convert list of peers ids to sha1 hash
		List<String> hashed_peers = new ArrayList<String>();
		for (int i = 0; i < size; i++) {
			String p = peer_ids.get(i);
			p = sha1(p);
			hashed_peers.add(p);
		}

		ArrayList<String> matched_peers = new ArrayList<String>();
		for (int length = 40; length >= 0; length--) {
			for (int i = 0; i < size; i++) {
				if (hash.regionMatches(true, 0, hashed_peers.get(i), 0, length)) {
					flag = true;
					matched_peers.add(hashed_peers.get(i));
				}
			}
			if (flag) {
				length = (length <= 0) ? 0 : length - 1;
				for (int j = 0; j < matched_peers.size(); j++) {
					String peer_str = ""
							+ Character.toUpperCase(matched_peers.get(j)
									.charAt(length));
					String hash_str = ""
							+ Character.toUpperCase(hash.charAt(length));
					int peer_num = Integer.parseInt(peer_str, 16);
					int hash_num = Integer.parseInt(hash_str, 16);

					if (peer_num - hash_num < min) {
						min = peer_num - hash_num;
						idx = j;
					}
				}
				break;
			}
		}
		int index = hashed_peers.indexOf(matched_peers.get(idx));
		best = peer_ids.get(index);
		return best;
	}
}