package ir.xweb.data;

import ir.xweb.util.MimeType;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class XmlFormatter2 implements Formatter {

    @Override
    public void write(final Writer writer, final Object object) throws IOException {
        if(writer == null) {
            throw new IllegalArgumentException("null writer");
        }
        if(object == null) {
            throw new IllegalArgumentException("null object");
        }

        final Document document = new Document(Arrays.asList(toElement(object)));
        final XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        xmlOutput.output(document, writer);
    }

    private Element toElement(final Object object) {
        if(object instanceof Map) {
            final Map<?, ?> map = (Map) object;
            final String name = (object instanceof AnnotedMap) ? ((AnnotedMap) object).name : "Map";
            final Element element = new Element(name);
            for(Map.Entry e:map.entrySet()) {
                final Element sub = toElement(e.getValue());
                sub.setAttribute("key", e.getKey().toString());

                element.addContent(sub);
            }
            return element;
        }
        else if(object instanceof Collection) {
            final Collection<?> collection = (Collection) object;
            final Element element = new Element("Collection");
            for(Object o:collection) {
                final Content c = toElement(o);
                element.addContent(c);
                /*if(c instanceof Text) {
                    final Element sub = new Element(o.getClass().getSimpleName());
                    sub.addContent(c);

                    element.addContent(sub);
                } else {
                    final List<?> childs = ((Element)c).getChildren();
                    for(int i=childs.size()-1; i>=0; i--) {
                        final Content child = (Content) childs.get(i);
                        child.detach();
                        element.addContent(child);
                    }
                }*/
            }
            return element;
        }
        else {
            final Element element = new Element(getType(object));
            element.addContent(object.toString());
            return element;
        }
    }

    private String getType(final Object o) {
        if(o instanceof Map) {
            return "Map";
        }
        else if(o instanceof Collection) {
            return "Collection";
        }
        return o.getClass().getSimpleName();
    }

    @Override
    public String getMimeType() {
        return MimeType.get("xml");
    }

}
