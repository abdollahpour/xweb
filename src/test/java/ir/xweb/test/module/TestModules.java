
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.test.module;

import org.testng.annotations.Test;

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
        final TestModuleParameters test = new TestModuleParameters();
        test.test(getManager());
    }

    @Test
    public void testJsonPost() throws IOException {
        final TestJsonPost post = new TestJsonPost();
        post.test();
    }

    @Test
    public void testGZipModule() throws IOException, ServletException {
        final TestGzipModule gzip = new TestGzipModule(this);
        gzip.test(true);
        gzip.test(false);
    }

}
