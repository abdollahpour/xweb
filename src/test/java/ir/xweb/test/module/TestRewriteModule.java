
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.test.module;

import ir.xweb.module.RewriteModule;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class TestRewriteModule {

    private final TestModules modules;

    public TestRewriteModule(final TestModules modules) {
        this.modules = modules;
    }

    public void test1() throws IOException, ServletException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/redirect?param1=1");

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final TestChain chain = new TestChain();

        final RewriteModule emptyModule = modules.getManager().getModuleOrThrow(RewriteModule.class);
        emptyModule.doFilter(this.modules.getServletContext(), request, response, chain);

        verify(response, times(1)).sendRedirect(eq("/success-redirect?param1=1"));
    }

    public void test2() throws IOException, ServletException {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/users/xml");

        final HttpServletResponse response = mock(HttpServletResponse.class);
        final TestChain chain = new TestChain();

        final RewriteModule emptyModule = modules.getManager().getModuleOrThrow(RewriteModule.class);
        emptyModule.doFilter(this.modules.getServletContext(), request, response, chain);

        verify(response, times(1)).sendRedirect(eq("/users?api=users&users&format=xml"));
    }

    private class TestChain implements FilterChain {

        ServletRequest request;

        ServletResponse response;

        @Override
        public void doFilter(
                final ServletRequest servletRequest,
                final ServletResponse servletResponse) throws IOException, ServletException
        {
            this.request = servletRequest;
            this.response = servletResponse;
        }

    }

}
