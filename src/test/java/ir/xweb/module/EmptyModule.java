package ir.xweb.module;

import org.apache.commons.fileupload.FileItem;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

public class EmptyModule extends Module {

    public EmptyModule(Manager manager, ModuleInfo info, ModuleParam properties) throws ModuleException {
        super(manager, info, properties);
    }

    @Override
    public void process(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final ModuleParam param,
            final HashMap<String, FileItem> files) throws IOException {

        System.out.println(param.getStrings("test")[0]);
    }
}
