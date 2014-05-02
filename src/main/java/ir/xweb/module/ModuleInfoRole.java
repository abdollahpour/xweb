package ir.xweb.module;

import java.util.List;

public interface ModuleInfoRole {

    String param();

    String match();

    String accept();

    String reject();

    List<ModuleInfoRole> or();

    List<ModuleInfoRole> and();

}
