
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import java.util.List;

/**
 * Module information.
 */
public interface ModuleInfo {

    /**
     * Module name.
     * @return name
     */
	String getName();

    /**
     * Author name.
     * @return Name
     */
	String getAuthor();

    /**
     * List of validator for this module.
     * @return validators
     */
    List<ModuleInfoValidator> getValidators();

    /**
     * List of roles for this module.
     * @return roles.
     */
    List<ModuleInfoRole> getRoles();

}
