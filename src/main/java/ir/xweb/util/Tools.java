package ir.xweb.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Tools {

	/**
	 * Version of lastIndexOf that uses regular expressions for the search by
	 * Julian Cochran.
	 */
	public static int lastIndexOfRegex(String message, String regex) {
		// Need to add an extra character to message because to ensure
		// split works if toFind is right at the end of the message.
		message = message + " ";
		String separated[] = message.split(regex);
		if (separated == null || separated.length == 0 || separated.length == 1) {
			return -1;
		}
		return separated[separated.length - 1].length();
	}

	/**
	 * Version of indexOf that uses regular expressions for the search by Julian
	 * Cochran.
	 */
	public static int indexOf(String text, String regex) {
		// Need to add an extra character to message because to ensure
		// split works if toFind is right at the end of the message.

		text = text + " ";
		String separated[] = text.split(regex);
		if (separated == null || separated.length == 0 || separated.length == 1) {
			return -1;
		}
		return separated[0].length();
	}

}
