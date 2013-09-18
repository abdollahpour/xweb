/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import java.util.Collections;

import static org.mockito.Mockito.mock;

public class TestModule {

    final ServletContext servletContext = mock(ServletContext.class);

    final Manager manager = mock(Manager.class);

    final ModuleInfo moduleInfo = mock(ModuleInfo.class);

    final ModuleParam moduleParam = new ModuleParam(Collections.EMPTY_MAP);

    final HttpSession session = mock(HttpSession.class);

}
