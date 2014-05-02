package ir.xweb.test.module;

import ir.xweb.data.DataTools;
import ir.xweb.module.Manager;
import ir.xweb.module.Module;
import ir.xweb.module.ModuleException;
import ir.xweb.module.ModuleInfo;
import ir.xweb.module.ModuleParam;
import org.apache.commons.fileupload.FileItem;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

public class ReplyModule extends Module {


    public ReplyModule(Manager manager, ModuleInfo info, ModuleParam properties) throws
        ModuleException
    {
        super(manager, info, properties);
    }

    @Override
    public void process(
            final ServletContext context,
            final HttpServletRequest request,
            final HttpServletResponse response,
            final ModuleParam param,
            final HashMap<String, FileItem> files) throws IOException {

        new DataTools().write(response, "json", null, param);
    }
}
