/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.module;

import java.util.List;
import java.util.Map;

public interface ModuleInfo {
	
	String getName();
	
	String getAuthor();

    List<ModuleInfoValidator> getValidators();

    List<ModuleInfoRole> getRoles();

}
