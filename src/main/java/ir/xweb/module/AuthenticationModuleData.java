
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import ir.xweb.server.XWebUser;

/**
 * Authentication module data source. Implement this interface into you module to be able to
 * handle authentication by Authentication module.
 */
public interface AuthenticationModuleData {

    /**
     * Generate UUID login code for user and return it. It should also store this code
     * automatically.
     * @param userId UserId
     * @return UUID code or null if it's not possible
     */
    String generateUUID(final String userId);

    /**
     * Get user by UUID code.
     * @param uuid UUID code
     * @return User or null if code is not valid
     */
    XWebUser getUserWithUUID(final String uuid);

    /**
     * Get User by userId and password.
     * @param userId User ID
     * @param pass password
     * @return User or null if userId or password is not right
     */
    XWebUser getUserWithId(final String userId, final String pass);

}
