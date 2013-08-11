package ir.xweb.module;

import ir.xweb.server.XWebUser;

import javax.servlet.ServletContext;
import java.io.IOException;

public abstract class UserDataSource implements DataSource {

    public final static String DATA_SOURCE_USER_AUTH_PASS = "data-user-auth-pass";

    public final static String DATA_SOURCE_USER_AUTH_REMEMBER_CODE = "data-user-auth-remember-code";

    public final static String DATA_SOURCE_USER_REMEMBER = "data-user-remember";

    @Override
    public Object getData(ServletContext context, String type, Object... query) {
        if(DATA_SOURCE_USER_AUTH_PASS.equals(type)) {
            String user = (String) query[0];
            String pass = (String) query[1];

            return getUserWithPass(context, user, pass);
        } else if(DATA_SOURCE_USER_AUTH_REMEMBER_CODE.equals(type)) {
            String code = (String) query[0];

            return getUserWithRememberCode(context, code);
        } else if(DATA_SOURCE_USER_REMEMBER.equals(type)) {
            String identifier = (String) query[0];
            boolean generate = query.length > 1 ? (Boolean) query[1] : false;

            if(generate) {
                return  generateRememberUuid(context, identifier);
            } else {
                return getRememberUuid(context, identifier);
            }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setData(ServletContext context, String type, Object... query) throws IOException {
        throw new IllegalStateException("You can not set data!");
    }

    public abstract XWebUser getUserWithPass(ServletContext context, String identifier, String md5Password);

    public abstract XWebUser getUserWithRememberCode(ServletContext context, String rememberCode);

    public abstract String getRememberUuid(ServletContext context, String identifier);

    public abstract String generateRememberUuid(ServletContext context, String identifier);

}
