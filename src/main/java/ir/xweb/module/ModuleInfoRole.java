package ir.xweb.module;

import java.util.List;

public interface ModuleInfoRole {

    List<String> getParams();

    String getRole();

    String getEval();

    boolean definite();

}
