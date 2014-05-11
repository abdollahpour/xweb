package ir.xweb.test.data;


import ir.xweb.data.DataTools;
import ir.xweb.data.XmlFormatter2;
import org.junit.Test;


import java.io.IOException;
import java.util.*;

public class TestXmlFormater2 {

    @Test
    public void testWriterXml() throws IOException {
        final List list = new ArrayList();
        list.add("item1");
        list.add("item2");
        list.add("item3");
        list.add(Collections.singletonMap("key", "value"));

        final DataTools d = new DataTools();
        d.addFormatter(DataTools.FORMAT_XML, new XmlFormatter2());
        System.out.println(d.write("xml", null, list));
    }

    @Test
    public void testWriterXml2() throws IOException {
        final List list = new ArrayList();
        list.add("item1");
        list.add("item2");
        list.add("item3");

        final Map map = new HashMap();
        map.put("test1", "value1");
        map.put("items", list);

        final DataTools d = new DataTools();
        d.addFormatter(DataTools.FORMAT_XML, new XmlFormatter2());
        System.out.println(d.write("xml", null, map));
    }

}
