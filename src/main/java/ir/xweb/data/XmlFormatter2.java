package ir.xweb.data;

import ir.xweb.util.MimeType;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class XmlFormatter2 implements DataTools.Formatter {

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
        xmlOutput.setFormat(Format.getCompactFormat());
        xmlOutput.output(document, writer);
    }

    private Content toElement(final Object object) {
        if(object instanceof Map) {
            final Map<?, ?> map = (Map) object;
            final Element element = new Element("Map");
            for(Map.Entry e:map.entrySet()) {
                final Element sub = new Element(e.getKey().getClass().getSimpleName());
                sub.setAttribute("key", e.getKey().toString());
                sub.setAttribute("type", e.getValue().getClass().getSimpleName());

                final Content c = toElement(e.getValue());
                if(c instanceof Text) {
                    sub.addContent(c);
                } else {
                    final List<?> childs = ((Element)c).getChildren();
                    for(int i=childs.size()-1; i>=0; i--) {
                        final Content child = (Content) childs.get(i);
                        child.detach();
                        sub.addContent(child);
                    }
                }

                element.addContent(sub);
            }
            return element;
        }
        else if(object instanceof Collection) {
            final Collection<?> collection = (Collection) object;
            final Element element = new Element("Collection");
            for(Object o:collection) {
                final Content c = toElement(o);
                if(c instanceof Text) {
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
                }
            }
            return element;
        }
        else {
            final Text t = new Text(object.toString());
            return t;
        }
    }

    @Override
    public String getMimeType() {
        return MimeType.get("xml");
    }

}
