package ir.xweb.test.util;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;


/**
 * @author Hamed Abdollahpour
 * @version 1.0
 */
public class XmlBundle extends ResourceBundle {

    private HashMap<String, String> items = new HashMap<String, String>();

    public XmlBundle(File file) throws IOException {
        this(file.toURI().toURL());
    }

    public XmlBundle(URL url) throws IOException {
        this(url.openStream());
    }

    public XmlBundle(InputStream is) throws IOException {
        try {
            final SAXBuilder builder = new SAXBuilder();

            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            final Document document = (Document) builder.build(reader);

            final List list = document.getRootElement().getChildren("string");

            for(Object element:list) {
                final Element e = (Element) element;

                final String name = e.getAttributeValue("name");
                String text = e.getText();

                text = replace(text, "\\n", "\n");
                text = replace(text, "\\\\", "\\");

                items.put(name, text);
            }
        } catch (JDOMException ex) {
            throw new IOException(ex);
        }
    }

    public static String getOsName() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.indexOf("mac") >= 0) {
            return "mac";
        } else if (osName.indexOf("win") >= 0) {
            return "win";
        } else { //assume Unix or Linux
            return "lin";
        }
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(items.keySet());
    }

    @Override
    protected Object handleGetObject(String arg) {
        return items.get(arg);
    }

    private String replace(final String source, final String toReplace, final String replacement) {
        String result = source;
        int idx = source.lastIndexOf( toReplace );
        if ( idx != -1 ) {
            final StringBuffer ret = new StringBuffer( source );
            ret.replace( idx, idx+toReplace.length(), replacement);
            while ((idx = source.lastIndexOf(toReplace, idx - 1)) != -1) {
                ret.replace(idx, idx + toReplace.length(), replacement );
            }
            result = ret.toString();
        }

        return result;
    }

}