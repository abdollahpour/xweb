/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.server;

import ir.xweb.module.Manager;

import java.io.InputStream;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XWebLoader implements ServletContextListener {
	
	private Logger logger = LoggerFactory.getLogger(XWebLoader.class);
	
	private static Manager manager;

	@Override
	public void contextInitialized(ServletContextEvent context) {
        if(manager == null) {
            manager = new Manager(context.getServletContext());
        }

		try {
			InputStream in = context.getServletContext().getResourceAsStream("/WEB-INF/xweb.xml");

			
			manager.load(in);
			
			logger.debug(manager.getModules().size() + " module successfully loaded");
			for(String name:manager.getModules().keySet()) {
				logger.debug("\t\t" + name + ": " + manager.getModules().get(name).getClass());
			}
			
			context.getServletContext().setAttribute(Constants.SESSION_MANAGER, manager);
		} catch (Exception ex) {
			logger.error("Error in load modules", ex);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		manager.destroy();
	}
}
