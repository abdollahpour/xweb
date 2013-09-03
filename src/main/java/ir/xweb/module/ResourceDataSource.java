package ir.xweb.module;

import javax.servlet.ServletContext;

public abstract class ResourceDataSource implements DataSource {

    public final static String DATA_SOURCE_STORE = "data-resource-store";

    public final static String DATA_SOURCE_USER_AUTH_REMEMBER_CODE = "data-user-auth-remember-code";

    public final static String DATA_SOURCE_USER_REMEMBER = "data-user-remember";

    @Override
    public Object getData(ServletContext context, String type, Object... query) {
        if(DATA_SOURCE_STORE.equals(type)) {
            String path = (String) query[0];

            return addDataUsage(context, path);
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public abstract int addDataUsage(ServletContext context, String path);

}
