/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.*;

@RunWith(value = Parameterized.class)
public class TestResourceModuleValidPath extends TestModule {

    final String path;

    public TestResourceModuleValidPath(final String path) {
        this.path = path;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { "/" }, { "/test1" }, { "test2" } };
        return Arrays.asList(data);
    }

    @Before
    public void setup() {
        when(manager.getContext()).thenReturn(servletContext);
        when(servletContext.getInitParameter("data_store_path")).thenReturn("/tmp");
    }

    @Test
    public void testResourceModuleIllegalPath() {
        final ResourceModule resourceModule = new ResourceModule(manager, moduleInfo, moduleParam);

        resourceModule.getFile("1", this.path);
    }



}
