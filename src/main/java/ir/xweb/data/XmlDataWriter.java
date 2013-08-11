package ir.xweb.data;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.IOException;
import java.io.Writer;

public class XmlDataWriter implements DataWriter {

    private Element root;

    private Element element;

    @Override
    public void write(Writer writer) throws IOException {
        Document document = new Document(root);
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getCompactFormat());
        xmlOutput.output(document, writer);
    }

    @Override
    public void add(String key, Object value) {
        if(element == null) {
            throw new IllegalArgumentException("Please start first");
        } else {
            Element e = new Element(key);
            e.setText(value.toString());
            element.addContent(e);
        }
    }

    @Override
    public void start(String name) {
        Element newElement = new Element(name);
        if(root == null) {
            root = newElement;
        }

        if(element != null) {
            element.addContent(newElement);
        }
        element = newElement;
    }

    @Override
    public void startArray(String name) {
        start(name);
    }

    @Override
    public void end() {
        if(element != null) {
            element = element.getParentElement();
        } else {
            throw new IllegalStateException("No active node");
        }
    }

    @Override
    public void release() {
        element = null;
        root = null;
    }

    @Override
    public String getContentType() {
        return "text/xml;charset=utf-8";
    }
}
