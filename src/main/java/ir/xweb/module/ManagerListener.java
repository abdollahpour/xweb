
/**
 * XWeb project
 * https://github.com/abdollahpour/xweb
 * Hamed Abdollahpour - 2013
 */

package ir.xweb.module;

import javax.servlet.ServletContext;

/**
 * Listen events from manager
 */
public interface ManagerListener {

    /**
     * Rise an event when new schedule run. this method will run on Schedule thread.
     * @param context Context
     * @param module Module
     * @param params Schedule params
     */
    void startSchedule(ServletContext context, Module module, ModuleParam params);

    /**
     * Rise an event when new schedule finish. this method will run on Schedule thread.
     * @param context Context
     * @param module Module
     * @param params Schedule params
     */
    void finishSchedule(ServletContext context, Module module, ModuleParam params);

}
