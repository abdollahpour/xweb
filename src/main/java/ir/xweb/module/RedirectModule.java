/**
 * XWeb project
 * https://github.com/abdollahpour/xweb
 * Hamed Abdollahpour - 2013
 */

package ir.xweb.module;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RedirectModule extends Module {

    public RedirectModule(
            final Manager manager,
            final ModuleInfo info,
            final ModuleParam properties) throws ModuleException {

        super(manager, info, properties);
    }

    @Override
    public void doFilter(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws IOException, ServletException {

        String uri = request.getRequestURI();

        String[] uriParts = uri.split("[#?]");
        String path = uriParts[0];
        String rest = uri.substring(uriParts[0].length());

        if(getProperties().containsKey(path)) {
            response.sendRedirect(getProperties().get(path) + rest);
        } else {
            filterChain.doFilter(request, response);
        }
    }

}
