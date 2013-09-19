/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.data;

import static org.mockito.Mockito.*;

import ir.xweb.module.AuthenticationModule;
import ir.xweb.module.CaptchaModule;
import ir.xweb.module.ModuleException;
import ir.xweb.module.ModuleParam;
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

@RunWith(value = Parameterized.class)
public class TestDataTools {

    private final String format;

    private final Object data;

    private final DataTools tools = new DataTools();

    private final HttpServletResponse response = mock(HttpServletResponse.class);

    public TestDataTools(final String format, final Object data) {
        this.format = format;
        this.data = data;
    }

    @Before
    public void setup() throws IOException {
        // setup require values
        when(response.getWriter()).thenReturn(new PrintWriter(System.out));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<String> list1 = Arrays.asList("hamed", "ali", "muhammad");

        Map map1 = new HashMap();
        map1.put("users", list1);
        map1.put("count", 100);

        Map map2 = new HashMap();
        map2.put("name", "hamed");
        map2.put("family", "abdollahpour");
        map2.put("sub", map1);

        Object[][] data = new Object[][] {
                {"xml", map2},
                {"json", map2},
        };
        return Arrays.asList(data);
    }

    @Test
    public void writeWriter() throws IOException {
        tools.write(response, this.format, null, this.data);
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

        @XWebDataElement (role = "admin,user", writable = true, validator = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$")
        public String email;

    }

}
