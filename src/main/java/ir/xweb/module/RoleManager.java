package ir.xweb.module;

import ir.xweb.server.Constants;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoleManager {

    //private final static String DEFAULT_ROLE = Constants.ROLE_GUEST;

    //private final static String PARAM_VALIDATOR = "^[A-Za-z]{1,20}$";

    private List<ModuleInfoRole> roles = new ArrayList<ModuleInfoRole>();

    private ScriptEngine engine;

    public RoleManager(List<ModuleInfoRole> roles) {
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("js");
        this.roles = roles;
    }

    public boolean hasPermission(Map<String, String> params, String role) throws ScriptException {
        if(roles.size() > 0) {  /* We accept all the request if we do not have any role */
            boolean results = false;

            for(ModuleInfoRole v:roles) {
                boolean roleMatch;
                if(role == null) {
                    roleMatch = (v.getRole().length() == 0) || "".matches(v.getRole());
                } else {
                    roleMatch = role.matches(v.getRole());
                }

                // Empty roll, means all the rolls
                if(roleMatch) {
                    boolean paramMatches;

                    if(v.getParams().size() > 0) {
                        paramMatches = false;

                        for(String paramName:v.getParams()) {
                            if(params.containsKey(paramName)) {
                                paramMatches = true;
                                break;
                            }
                        }
                    } else {
                        paramMatches = true;
                    }

                    if(paramMatches) {
                        String eval = v.getEval();
                        for(String p:v.getParams()) {
                            String value = params.get(p);

                            if(value != null) {
                                // scrip illegal character
                                value = value.replace("\n", "\\n");
                                value = value.replace("'", "\'");
                            } else {
                                value = "";
                            }

                            eval = eval.replace("%" + p + "%", "'" + value + "'");
                        }

                        boolean result = result(engine.eval(eval));
                        if(result == false) {
                            return false;
                        } else if(v.definite()) {
                            return true;
                        } else {
                            results = true;
                        }
                    }
                }
            }

            return results;
        }

        return true; // no role
    }

    private boolean result(Object result) {
        if(result == null) {
            return false;
        }

        String s = result.toString();
        if("true".equalsIgnoreCase(s)) {
            return true;
        } else if("false".equalsIgnoreCase(s)) {
            return false;
        }

        return !s.matches("[0.]+$");
    }

}
