
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.test.module;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Test module with xweb.xml file.
 */
public class TestModules extends TestModule {

    /**
     * Test module parameters.
     */
    @Test
    public void testModuleParameters() {
        final TestModuleParameters test = new TestModuleParameters();
        test.test(getManager());
    }

    @Test
    public void testJsonPost() throws IOException {
        final TestJsonPost post = new TestJsonPost();
        post.test();
    }

}
