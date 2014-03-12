package ir.xweb.module;

import javax.script.ScriptException;
import java.util.List;
import java.util.Map;

public class RoleManager {

    private final List<ModuleInfoRole> roles;

    public RoleManager(final List<ModuleInfoRole> roles) {
        this.roles = roles;
    }

    public boolean hasPermission(final Map<String, String> params, final String role) throws ScriptException {
        final String _role = role == null ? "" : role;
        
        for(ModuleInfoRole r:roles) {
            for(Map.Entry<String, String> e:params.entrySet()) {
                if(e.getKey().matches(r.param())) {
                    if(r.match() == null || r.match().matches(e.getValue())) {
                        if(r.accept() != null && !_role.matches(r.accept())) {
                            return false;
                        }
                        if(r.reject() != null && _role.matches(r.reject())) {
                            return false;
                        }
                    }
                }
            }
        }

        return true; // no role
    }

}
