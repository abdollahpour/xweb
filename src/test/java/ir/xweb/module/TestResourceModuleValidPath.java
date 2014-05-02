/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import java.io.IOException;

//@RunWith(value = Parameterized.class)
public class TestResourceModuleValidPath extends TestModule {

    final String path;

    public TestResourceModuleValidPath(final String path) throws IOException {
        this.path = path;
    }

    /*@Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { "/" }, { "/test1" }, { "test2" } };
        return Arrays.asList(data);
    }

    @Before
    public void setup() {
        //when(servletContext.getInitParameter("data_store_path")).thenReturn("/tmp");
    }

    @Test
    public void testResourceModuleIllegalPath() throws ModuleException {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(ResourceModule.PROPERTY_DATA_DIR, new File(".").getPath());

        ModuleParam moduleParam = new ModuleParam(params);

        final ResourceModule resourceModule = new ResourceModule(manager, this.moduleInfo, moduleParam);

        resourceModule.getFile("1", this.path);
    }*/



}
