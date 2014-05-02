
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.module;

import org.testng.annotations.Test;

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

}
