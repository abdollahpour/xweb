/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.module;

import java.io.IOException;

import javax.servlet.ServletContext;

public interface DataSource {

	public Object getData(ServletContext context, String type, Object... query);
	
	public void setData(ServletContext context, String type, Object... query) throws IOException;
	
}