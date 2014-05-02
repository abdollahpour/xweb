package ir.xweb.util;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    /**
     * @deprecated please use {@link #gzip(java.io.File, java.io.File) zip(java.io.File, java.io.File)}
     */
    @Deprecated
    public static void zipFile(File src, File dist) throws IOException {
        gzip(src, dist);
    }

    /**
     * Compress one specific file with gzip
     * @param src Source file
     * @param dist Destination file. Parent directory of this file should exist
     * @throws IOException
     */
    public static void gzip(final File src, final File dist) throws IOException {
        if(src == null) {
            throw new IllegalArgumentException("null src");
        }
        if(!src.exists() || !src.isFile()) {
            throw new IllegalArgumentException("Src file does not exist of it's not file");
        }

        if(dist == null) {
            throw new IllegalArgumentException("dist src");
        }

        FileInputStream fis = null;
        GZIPOutputStream gzos = null;

        try {
            fis = new FileInputStream(src);
            gzos = new GZIPOutputStream(new FileOutputStream(dist));

            int size;
            byte[] buffer = new byte[20480];

            while((size = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, size);
            }
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (Exception ex) {}
            }
            if(gzos != null) {
                try {
                    gzos.flush();
                    gzos.finish();
                    gzos.close();
                } catch (Exception ex){}
            }
        }
    }

    public static String getUrlParam(final String queryString, final String encoding, final String name) throws UnsupportedEncodingException {
        final Map<String, String[]> params = parseQueryString(queryString, encoding);
        final String[] values = params.get(name);
        if(values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }

    @Deprecated
    public static HashMap<String, List<String>> parseQueryString(String queryString) {
        final Map<String, String[]> params = parseQueryString(queryString, "utf-8");
        final HashMap<String, List<String>> map = new HashMap<String, List<String>>(params.size());

        for(Map.Entry<String, String[]> e:params.entrySet()) {
            map.put(e.getKey(), Arrays.asList(e.getValue()));
        }

        return map;
    }

    public static Map<String,String[]> parseQueryString(
            final String data,
            final String encoding) {

        if ((data != null) && (data.length() > 0)) {

            final Map<String, String[]> map = new HashMap<String, String[]>();

            // use the specified encoding to extract bytes out of the
            // given string so that the encoding is not lost. If an
            // encoding is not specified, let it use platform default
            byte[] bytes = null;
            try {
                if (encoding == null) {
                    bytes = data.getBytes();
                } else {
                    bytes = data.getBytes(encoding);
                }
                parseParameters(map, bytes, encoding);

                return map;
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalArgumentException(ex);
            }

        }

        return Collections.emptyMap();
    }

    private static void parseParameters(
            final Map<String,String[]> map,
            final byte[] data,
            final String encoding) throws UnsupportedEncodingException {

        if (data != null && data.length > 0) {
            int    ix = 0;
            int    ox = 0;
            String key = null;
            String value = null;
            while (ix < data.length) {
                byte c = data[ix++];
                switch ((char) c) {
                    case '&':
                        value = new String(data, 0, ox, encoding);
                        if (key != null) {
                            putMapEntry(map, key, value);
                            key = null;
                        }
                        ox = 0;
                        break;
                    case '=':
                        if (key == null) {
                            key = new String(data, 0, ox, encoding);
                            ox = 0;
                        } else {
                            data[ox++] = c;
                        }
                        break;
                    case '+':
                        data[ox++] = (byte)' ';
                        break;
                    case '%':
                        data[ox++] = (byte)((convertHexDigit(data[ix++]) << 4)
                                + convertHexDigit(data[ix++]));
                        break;
                    default:
                        data[ox++] = c;
                }
            }
            //The last value does not end in '&'.  So save it now.
            if (key != null) {
                value = new String(data, 0, ox, encoding);
                putMapEntry(map, key, value);
            }
        }

    }

    private static byte convertHexDigit(final byte b) {
        if ((b >= '0') && (b <= '9')) return (byte)(b - '0');
        if ((b >= 'a') && (b <= 'f')) return (byte)(b - 'a' + 10);
        if ((b >= 'A') && (b <= 'F')) return (byte)(b - 'A' + 10);
        throw new IllegalArgumentException();
    }

    private static void putMapEntry(
            final Map<String, String[]> map,
            final String name,
            final String value) {

        String[] newValues = null;
        final String[] oldValues = map.get(name);
        if (oldValues == null) {
            newValues = new String[1];
            newValues[0] = value;
        } else {
            newValues = new String[oldValues.length + 1];
            System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
            newValues[oldValues.length] = value;
        }
        map.put(name, newValues);
    }

    /**
     * Calculate file checksum
     * @param file Source file
     * @return Hex string for checksum
     * @throws IOException
     */
    public static String checkSum(final File file) throws IOException {
        if(file == null) {
            throw new IllegalArgumentException("null file");
        }
        if(!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }

        FileInputStream fis = null;
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(file);

            byte[] array = new byte[10240];
            int size;

            while((size = fis.read(array)) > 0) {
                md.update(array, 0, size);
            }

            byte[] mdbytes = md.digest();

            //convert the byte to hex format
            StringBuffer sb = new StringBuffer("");
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException(ex);
        } finally {
            if(fis != null) {
                try {
                    fis.close();
                } catch (Exception ex) {}
            }
        }
    }

    public static String md5(String pass)  {
        if(pass != null) {
            try {
                MessageDigest algorithm = MessageDigest.getInstance("MD5");

                byte[] defaultBytes = pass.getBytes("UTF-8");
                algorithm.reset();
                algorithm.update(defaultBytes);
                byte messageDigest[] = algorithm.digest();

                StringBuffer hexString = new StringBuffer();

                for (int i = 0; i < messageDigest.length; i++) {
                    String hex = Integer.toHexString(0xFF & messageDigest[i]);
                    if (hex.length() == 1)
                    {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }

                return hexString.toString();
            } catch (NoSuchAlgorithmException ex) {
                // never happen
            } catch (UnsupportedEncodingException e) {
                // never happen
            }
        }
        return null;
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

    public static boolean isValidFilename(String filename) {
        if(System.getProperty("os.name").toLowerCase().contains("Windows")) {
            Pattern pattern = Pattern.compile(
                    "# Match a valid Windows filename (unspecified file system).          \n" +
                            "^                                # Anchor to start of string.        \n" +
                            "(?!                              # Assert filename is not: CON, PRN, \n" +
                            "  (?:                            # AUX, NUL, COM1, COM2, COM3, COM4, \n" +
                            "    CON|PRN|AUX|NUL|             # COM5, COM6, COM7, COM8, COM9,     \n" +
                            "    COM[1-9]|LPT[1-9]            # LPT1, LPT2, LPT3, LPT4, LPT5,     \n" +
                            "  )                              # LPT6, LPT7, LPT8, and LPT9...     \n" +
                            "  (?:\\.[^.]*)?                  # followed by optional extension    \n" +
                            "  $                              # and end of string                 \n" +
                            ")                                # End negative lookahead assertion. \n" +
                            "[^<>:\"/\\\\|?*\\x00-\\x1F]*     # Zero or more valid filename chars.\n" +
                            "[^<>:\"/\\\\|?*\\x00-\\x1F\\ .]  # Last char is not a space or dot.  \n" +
                            "$                                # Anchor to end of string.            ",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.COMMENTS);
            Matcher matcher = pattern.matcher(filename);
            boolean isMatch = matcher.matches();
            return isMatch;
        } else {
            return filename.matches("^[^.\\\\/:*?\"<>|]?[^\\\\/:*?\"<>|]*");
        }
    }

    public static String getFileExtension(final String f) {
        int i = f.lastIndexOf('.');
        if (i > 0) {
            return f.substring(i + 1);
        }

        return f;
    }

    public static String implode(final String glue, final Collection<?> parts) {
        final StringBuffer b = new StringBuffer();
        for(Object s:parts) {
            if(b.length() != 0) {
                b.append(glue);
            }
            b.append(s);
        }
        return b.toString();
    }

    public static String implode(final String glue, final Object... parts) {
        return implode(glue, Arrays.asList(parts));
    }

    @Deprecated
    public static String implode(final Collection<?> c, final String glue) {
        final StringBuffer b = new StringBuffer();
        for(Object s:c) {
            if(b.length() != 0) {
                b.append(glue);
            }
            b.append(s);
        }
        return b.toString();
    }
}
