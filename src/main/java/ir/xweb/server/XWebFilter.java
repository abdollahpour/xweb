package ir.xweb.server;

import ir.xweb.module.Manager;
import ir.xweb.module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

public class XWebFilter implements Filter {

    private final static Logger logger = LoggerFactory.getLogger("XWebFilter");

    private Collection<Module> modules;

    private ServletContext context;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        final Manager manager = (Manager) filterConfig.getServletContext().getAttribute(Constants.SESSION_MANAGER);
        if(manager != null) {
            modules = manager.getModules().values();
            for(Module m:modules) {
                m.initFilter(filterConfig);
            }
        } else {
            modules = null;
            logger.error("Manager not found in session!");
        }

        context = filterConfig.getServletContext();
    }

    @Override
    public void doFilter(
            final ServletRequest request,
            final ServletResponse response,
            final FilterChain filterChain) throws IOException, ServletException {

        // Damn servlet!
        // http://stackoverflow.com/questions/469874/how-do-i-correctly-decode-unicode-parameters-passed-to-a-servlet
        // We should to add to server.xml too:
        // <Connector    URIEncoding="UTF-8"    connectionTimeout="20000" port="8080" protocol="HTTP/1.1" ...>
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        ServletRequest req = request;
        ServletResponse resp = response;

        if(modules != null) {
            for(Module m:modules) {
                if(request instanceof HttpServletRequest) {
                    if(response instanceof HttpServletResponse) {
                        Chain chain = new Chain();
                        chain.chainedRequest = req;
                        chain.chainedResponse = resp;

                        m.doFilter(
                                context,
                                (HttpServletRequest) req,
                                (HttpServletResponse) resp,
                                chain
                        );

                        if(!chain.fired) {
                            return; // fail
                        }
                        req = chain.chainedRequest;
                        resp = chain.chainedResponse;
                    }
                }
            }
        }

        filterChain.doFilter(req, resp);
    }

    @Override
    public void destroy() {
        if(modules != null) {
            for(Module m:modules) {
                m.destroyFilter();
            }
        }
    }

    private class Chain implements FilterChain {

        boolean fired = false;

        ServletRequest chainedRequest;

        ServletResponse chainedResponse;

        @Override
        public void doFilter(
                ServletRequest request,
                ServletResponse response) throws IOException, ServletException {

            chainedRequest = request;
            chainedResponse = response;

            fired = true;
        }
    }

}
