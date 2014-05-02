
/**
 * XWeb project
 * Created by Hamed Abdollahpour
 * https://github.com/abdollahpour/xweb
 */

package ir.xweb.test.module;

import ir.xweb.module.Manager;
import ir.xweb.module.ModuleParam;

import static org.testng.Assert.*;

/**
 * Test module parameters from xweb.xml file.
 */
class TestModuleParameters {

    public void test(final Manager manager) {
        final EmptyModule module = manager.getModuleOrThrow(EmptyModule.class);
        assertEquals(module.getProperties().getString("param1"), "value1");

        final ModuleParam param2 = module.getProperties().getParam("param2");
        assertEquals(param2.getString("param21"), "value21");
        assertEquals(param2.getString("param22"), "value22");

        final String[] param3 = module.getProperties().getStrings("param3");
        assertEquals(param3[0], "value3[0]");
        assertEquals(param3[3], "value3[3]");

        final ModuleParam param4 = module.getProperties().getParam("param4");
        final Long[] param41 = param4.getLongs("param41");
        assertEquals(param41[0], new Long(411));
        assertEquals(param41[1], new Long(412));

        final Double[] param42 = param4.getDoubles("param42");
        assertEquals(param42[0], new Double(42.1D));
        assertEquals(param42[1], new Double(42.2D));
    }

}
