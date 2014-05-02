/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.test.module;

import java.util.List;

public interface ModuleInfo {
	
	String getName();
	
	String getAuthor();

    List<ModuleInfoValidator> getValidators();

    List<ModuleInfoRole> getRoles();

}
