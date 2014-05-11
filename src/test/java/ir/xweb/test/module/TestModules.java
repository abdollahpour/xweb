
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.test.module;


import org.junit.Test;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Test module with xweb.xml file.
 */
public class TestModules extends TestModule {

    /**
     * Test module parameters.
     */
    @Test
    public void testModuleParameters() throws IOException {
        final TestModuleParameters module = new TestModuleParameters();
        module.test(getManager());
    }

    @Test
    public void testJsonPost() throws IOException {
        final TestJsonPost module = new TestJsonPost();
        module.test();
    }

    @Test
    public void testGZipModule() throws IOException, ServletException {
        final TestGzipModule module = new TestGzipModule(this);
        module.test(true);
        module.test(false);
    }

}
