package ir.xweb.util;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

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

    public static String readTextFile(File file) throws IOException {
        if(file == null) {
            throw new IllegalArgumentException("null file");
        }

        // Max 20KB
        if(file.length() > 20 * 1000) {
            throw new IllegalArgumentException("Illegal file size");
        }

        StringBuilder s = new StringBuilder();

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);

            InputStreamReader r = new InputStreamReader(fis, "UTF-8");

            char[] buffer = new char[1024];
            int size = 0;

            while((size = r.read(buffer)) > 0)  {
                s.append(buffer, 0, size);
            }
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (Exception ex) {}
            }
        }

        return s.toString();
    }

    public static void gzipFile(File file, File zip) throws IOException {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        GZIPOutputStream zos = null;
        try {
            fis = new FileInputStream(file);
            fos = new FileOutputStream(zip);
            zos = new GZIPOutputStream(fos);

            byte[] buffer = new byte[10240];
            int size;

            while((size = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, size);
            }
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (Exception ex) {}
            }

            if(zos != null) {
                try {
                    zos.finish();
                    zos.flush();
                    zos.close();
                } catch (Exception ex) {}
            }

            if(fos != null) {
                try {
                    fos.flush();
                    fos.close();
                } catch (Exception ex) {}
            }
        }
    }

    public static void writeTextFile(String s, File file) throws IOException {
        if(file == null) {
            throw new IllegalArgumentException("null file");
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);

            OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8");

            w.write(s);
            w.flush();
        } finally {
            if(fos != null) {
                try {
                    fos.close();
                } catch (Exception ex) {}
            }
        }
    }

}
