/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.server;

import java.sql.Date;

public interface XWebRememberUser {

	public String getUsername();
    
    public String getUuid();
    
    public Date getDate();
	
}
