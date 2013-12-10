/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.data;

import static org.mockito.Mockito.*;

import ir.xweb.module.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class TestDataTools {


    private final DataTools tools = new DataTools();

    private final HttpServletResponse response = mock(HttpServletResponse.class);

    public TestDataTools() throws IOException {
        when(response.getWriter()).thenReturn(new PrintWriter(System.out));
    }

    @Test
    public void writeWriter() throws IOException {
        HashMap map = new HashMap();
        map.put("count", 10);

        List list = Arrays.asList(new User(), new User());
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

    @Test
    public void testUserWrite() throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final String email = "ha.hamed@gmail.com";

        final Map<String, String> map = new HashMap<String, String>();
        map.put("email", email);

        final User user = new User();

        tools.write(user, new ModuleParam(map), "admin");

        assertTrue(email.equals(user.email));
    }

    @XWebData(name = "user")
    class User {

        @XWebDataElement (role = "admin", writable = true, validator = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")
        public String email;

        @XWebDataElement(key = "userId")
        public long getUserId() {
            return 123;
        }

    }

}