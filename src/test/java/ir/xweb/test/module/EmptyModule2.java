package ir.xweb.test.module;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class EmptyModule2 extends Module {

    public EmptyModule2(
        final Manager manager,
        final ModuleInfo info,
        final ModuleParam properties) throws ModuleException
    {
        super(manager, info, properties);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(
        final ServletContext context,
        final HttpServletRequest request,
        final HttpServletResponse response,
        final ModuleParam param,
        final HashMap<String, FileItem> files) throws IOException {

        // write input to output
        byte[] buffer = new byte[1024];
        int size;

        final InputStream is = request.getInputStream();
        final OutputStream os = response.getOutputStream();

        while((size = is.read(buffer)) > 0) {
            os.write(buffer, 0, size);
        }
    }

}
