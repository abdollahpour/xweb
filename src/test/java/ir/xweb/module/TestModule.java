/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import org.junit.Before;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestModule {

    final ServletContext servletContext = mock(ServletContext.class);

    final ModuleInfo moduleInfo = mock(ModuleInfo.class);

    final ModuleParam moduleParam = new ModuleParam(Collections.EMPTY_MAP);

    final HttpSession session = mock(HttpSession.class);

    private final Manager manager;

    public TestModule() throws IOException {
        when(servletContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());

        manager = new Manager(servletContext);
        manager.load(getClass().getResource("/WEB-INF/xweb.xml"));
    }

    @Before
    public void setupServer() throws IOException {



    }

    public Manager getManager() {
        return this.manager;
    }

}
