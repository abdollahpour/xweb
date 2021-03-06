/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.test.data;

import ir.xweb.data.DataTools;
import ir.xweb.data.XWebData;
import ir.xweb.data.XWebDataElement;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class TestDataTools {


    private final DataTools tools = new DataTools();

    private final HttpServletResponse response = mock(HttpServletResponse.class);

    public TestDataTools() throws IOException {
        when(response.getWriter()).thenReturn(new PrintWriter(System.out));
    }

    public void testWriterJson() throws IOException {
        final HashMap map = new HashMap();
        map.put("count", 10);

        final List list = Arrays.asList(new User(), new User());
        map.put("users", list);

        System.out.println("Role: null");
        tools.write(response, "json", null, map);
        System.out.println();

        System.out.println("Role: admin");
        tools.write(response, "json", "admin", map);
        System.out.println();

        System.out.println("Role: user");
        tools.write(response, "json", "user", map);
        System.out.println();
    }

    public void testWriterXml1() throws IOException {
        final User user = new User();
        user.email = "ha.hamed@gmail.com";

        final HashMap map = new HashMap();
        map.put("count", 10);
        map.put("size", 101);
        map.put("list", Arrays.asList("this", "is", "test", 123, new Integer(456), user));

        final DataTools d = new DataTools();
        System.out.println(d.write("xml", null, map));
    }

    public void testWriterXml2() throws IOException {
        final List list = new ArrayList();
        list.add("item1");
        list.add("item2");
        list.add("item3");
        list.add(Collections.singletonMap("key", "value"));

        final DataTools d = new DataTools();
        System.out.println(d.write("xml", null, list));
    }

    public void testUserWrite() throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        /*final String email = "ha.hamed@gmail.com";

        final Map<String, String> map = new HashMap<String, String>();
        map.put("email", email);

        final User user = new User();

        tools.write(user, new ModuleParam(map), "admin");

        assertTrue(email.equals(user.email));*/
    }

    @XWebData(name = "user")
    class User {

        @XWebDataElement(read = "admin", writable = true, validator = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")
        public String email;

        @XWebDataElement(key = "userId")
        public long getUserId() {
            return 123;
        }

    }

}
