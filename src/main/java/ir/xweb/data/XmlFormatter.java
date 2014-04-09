package ir.xweb.data;

import ir.xweb.util.MimeType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

public class XmlFormatter implements Formatter {

    private final String mimeType = MimeType.get("xml");

    @Override
    public void write(final Writer writer, final Object object) throws IOException {
        //final String key = map.keySet().iterator().next().toString();
        //final Object value = map.get(key);
        if(object instanceof Map) {
            final Map<?, ?> map = (Map) object;
            if(map.size() > 0) {
                Element root;
                if(map.size() == 1) {
                    String key = map.keySet().iterator().next().toString();
                    root = new Element(key);
                    write(root, map.values().iterator().next());
                } else {
                    root = new Element("data");
                    write(root, map);
                }

                final Document document = new Document(root);
                final XMLOutputter xmlOutput = new XMLOutputter();
                xmlOutput.setFormat(Format.getCompactFormat());
                xmlOutput.output(document, writer);
            }
        } else if(object instanceof Collection) {
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

    private void write(Element parent, Object object) throws IOException {
        if(object instanceof Map) {

            final Map<?, ?> map =  (Map<?, ?>) object;
            for(Map.Entry<?, ?> e:map.entrySet()) {
                final Element element = new Element(e.getKey().toString());
                write(element, e.getValue());
                parent.addContent(element);
            }
        } else if(object instanceof Collection) {
            final Collection<?> list = (Collection<?>) object;

            for(Object o:list) {
                String key = null;
                if(o instanceof AnnotedMap) {
                    key = ((AnnotedMap)o).name.toLowerCase();
                }

                if(key == null || key.length() == 0) {
                    if(o instanceof String) {
                        key = "string";
                    } else if(o instanceof Integer) {
                        key = "integer";
                    } else if(o instanceof Float) {
                        key = "float";
                    } else if(o instanceof Long) {
                        key = "long";
                    } else if(o instanceof Double) {
                        key = "double";
                    } else {
                        key = o.getClass().getSimpleName();
                    }
                }

                Element e = new Element(key);
                write(e, o);
                parent.addContent(e);
            }
        } else {
            parent.setText(object.toString());
        }
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

}
