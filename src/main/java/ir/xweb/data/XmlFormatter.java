
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.data;

import ir.xweb.module.DataModule;
import ir.xweb.util.MimeType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

/**
 * Format XWeb object to XML.
 */
public class XmlFormatter implements Formatter {

    /**
     * Content type for this formatter.
     */
    private final String contentType = MimeType.get("xml");

    /**
     * {@inheritDoc}
     */
    @Override
    public final void write(final Writer writer, final Object object) throws IOException {
        if (object instanceof Map) {
            final Map<?, ?> map = (Map) object;
            if (map.size() > 0) {
                final Element root;
                if (map.size() == 1) {
                    String key = map.keySet().iterator().next().toString();

                    root = new Element(fixName(key));
                    write(root, map.values().iterator().next());
                }
                else {
                    root = new Element("data");
                    write(root, map);
                }

                final Document document = new Document(root);
                final XMLOutputter xmlOutput = new XMLOutputter();
                xmlOutput.setFormat(Format.getCompactFormat());
                xmlOutput.output(document, writer);
            }
        }
        else if (object instanceof Collection) {
            final Element root = new Element("data");
            write(root, object);

            final Document document = new Document(root);
            final XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getCompactFormat());
            xmlOutput.output(document, writer);
        } else {
            writer.write(object.toString());
        }
    }

    /**
     * Write an object to parent.
     * @param parent parent element
     * @param object xweb object
     * @throws IOException Error to write object
     */
    private void write(final Element parent, final Object object) throws IOException {
        if (object instanceof Map) {

            final Map<?, ?> map =  (Map<?, ?>) object;
            for (Map.Entry<?, ?> e:map.entrySet()) {
                final String name = e.getKey().toString();
                final String fixedName = fixName(name);

                final Element element = new Element(fixName(e.getKey().toString()));

                if(!fixedName.equals(name)) {
                    element.setAttribute("key", name);
                }

                write(element, e.getValue());
                parent.addContent(element);
            }
        }
        else if (object instanceof Collection) {
            final Collection<?> list = (Collection<?>) object;

            for (Object o:list) {
                String key = null;
                if (o instanceof AnnotedMap) {
                    key = ((AnnotedMap) o).name.toLowerCase();
                }

                if (key == null || key.length() == 0) {
                    if (o instanceof String) {
                        key = "string";
                    }
                    else if (o instanceof Integer) {
                        key = "integer";
                    }
                    else if (o instanceof Float) {
                        key = "float";
                    }
                    else if (o instanceof Long) {
                        key = "long";
                    }
                    else if (o instanceof Double) {
                        key = "double";
                    }
                    else {
                        key = o.getClass().getSimpleName();
                    }
                }

                final Element e = new Element(fixName(key));
                write(e, o);
                parent.addContent(e);
            }
        } else if(object != null) {
            parent.setText(object.toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getContentType() {
        return contentType;
    }

    /**
     * Fix XML tag name. We can just use English and numeric character for names. All the other
     * characters will replace by '_' character. Also if name start with 'XML' or numeric
     * character, '_' will add at the start of the name.
     * @param name Element name
     * @return Fixed name
     */
    private static String fixName(final String name) {
        String s = name.replaceAll("[^a-zA-Z0-9-_]", "_");
        if (s.matches("^((\\d)|(?i)(XML))+.*$")) {
            s = "_" + s;
        }

        return s;
    }

}
