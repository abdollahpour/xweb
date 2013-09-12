package ir.xweb.util;

import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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


    public static void zipFile(File src, File dest) throws IOException {
        FileInputStream fis = null;
        GZIPOutputStream gzos = null;

        try {
            fis = new FileInputStream(src);
            gzos = new GZIPOutputStream(new FileOutputStream(dest));

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
                    gzos.finish();
                    gzos.flush();
                    gzos.close();
                } catch (Exception ex){}
            }
        }
    }

    public static String getUrlParam(final String url, final String name) throws UnsupportedEncodingException {
        Map<String, List<String>> params = getUrlParameters(url);
        List<String> values = params.get(name);
        if(values != null && values.size() > 0) {
            return values.get(0);
        }
        return null;
    }

    public static Map<String, List<String>> getUrlParameters(final String url) throws UnsupportedEncodingException {
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String[] urlParts = url.split("\\?");
        if (urlParts.length > 1) {
            String query = urlParts[1];
            for (String param : query.split("&")) {
                String pair[] = param.split("=");
                String key = URLDecoder.decode(pair[0], "UTF-8");
                String value = "";
                if (pair.length > 1) {
                    value = URLDecoder.decode(pair[1], "UTF-8");
                }
                List<String> values = params.get(key);
                if (values == null) {
                    values = new ArrayList<String>();
                    params.put(key, values);
                }
                values.add(value);
            }
        }
        return params;
    }

}
