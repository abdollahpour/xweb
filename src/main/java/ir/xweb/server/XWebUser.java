/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.server;

public interface XWebUser {
	
	String getIdentifier();
	
	String getRole();

    Object getExtra();

}
