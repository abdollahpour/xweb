/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestModule {

    final ServletContext servletContext = mock(ServletContext.class);

    final ModuleInfo moduleInfo = mock(ModuleInfo.class);

    final ModuleParam moduleParam = new ModuleParam(Collections.EMPTY_MAP);

    final HttpSession session = mock(HttpSession.class);

    final Manager manager;

    public TestModule() throws IOException {
        when(servletContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());

        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                final File file = new File("src/test/webapp", args[0].toString());
                return file.getAbsolutePath();
            }
        }).when(servletContext).getRealPath(anyString());

        manager = new Manager(servletContext);
        manager.load(getClass().getResource("/WEB-INF/xweb.xml"));

        final Map<String, Module> modules = manager.getModules();
        for(Module module:modules.values()) {
            module.init(servletContext);
        }
        //for(Module module:module.values()) {
        //    module.initFilter();
        //}
    }

    public Manager getManager() {
        return manager;
    }

    public HttpSession getSession() {
        return session;
    }


}

