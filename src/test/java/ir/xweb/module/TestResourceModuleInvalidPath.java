/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import java.io.IOException;

//@RunWith(value = Parameterized.class)
public class TestResourceModuleInvalidPath extends TestModule {

    final String path;

    public TestResourceModuleInvalidPath(final String path) throws IOException {
        this.path = path;
    }

    /*@Parameterized.Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] { { (String)null }, { "./" }, { "../" }, { "~/" } };
        return Arrays.asList(data);
    }

    @Before
    public void setup() {

    }

    @Test(expected = IllegalArgumentException.class)
    public void testResourceModuleIllegalPath() throws ModuleException {
        final ResourceModule resourceModule = new ResourceModule(manager, moduleInfo, moduleParam);

        resourceModule.getFile("1", this.path);
    }*/



}
