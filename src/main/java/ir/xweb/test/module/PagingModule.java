package ir.xweb.test.module;

import ir.xweb.test.data.DataTools;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Please use DataModule instead
 */
@Deprecated
public class PagingModule extends Module {

    public final static String PARAM_PAGE_SIZE = "size";

    public final static String PARAM_PAGE_FORMAT = "format";

    public final static int DEFAULT_PAGE_SIZE = 100;

    public final static String DEFAULT_PAGE_FORMAT = "json";

    private final int pageSize;

    private final String format;

    private final DataTools dataTools = new DataTools();

    public PagingModule(
            final Manager manager,
            final ModuleInfo info,
            final ModuleParam properties) throws ModuleException {

        super(manager, info, properties);

        pageSize = properties.getInt(PARAM_PAGE_SIZE, DEFAULT_PAGE_SIZE);
        format = properties.getString(PARAM_PAGE_FORMAT, DEFAULT_PAGE_FORMAT);
    }

    public void writePage(
            final HttpServletResponse response,
            final ModuleParam params,
            final List<?> objects) throws IOException {
        writePage(response, params, objects, null);
    }

    public void writePage(
            final HttpServletResponse response,
            final ModuleParam params,
            final List<?> objects,
            final String role) throws IOException {

        final int page = params.getInt("page", 0);
        final int size = params.getInt("size", this.pageSize);
        final String format = params.getString("format", this.format);

        final int s = Math.min(Math.max(page, 0) * size, objects.size());
        final int l = Math.max(Math.min(size, 0), objects.size() - s);

        int count = objects.size() / size;
        if(count * size < objects.size()) {
            count++;
        }

        final List<Integer> pages = new ArrayList<Integer>();
        pages.add(0);
        if(count < 4) {
            for(int i=1; i<count; i++) {
                pages.add(i);
            }
        }
        else {
            int center = count / 2;
            if(center < 2) {
                center = 2;
            }

            pages.add(center - 1);
            pages.add(center);
            pages.add(center + 1);
            pages.add(count - 1);
        }

        final HashMap<Object, Object> results = new HashMap<Object, Object>();
        results.put("start", s);
        results.put("size", size);
        results.put("data", objects.subList(s, s + l));
        results.put("more", objects.size() > s + l);
        results.put("page", page);
        results.put("pages", pages);
        results.put("count", count);

        dataTools.write(response, format, role, results);
    }

}
