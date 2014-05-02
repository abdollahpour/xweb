
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
        final ModuleParam p = module.getProperties();
        
        final String param1 = p.getString("param1");
        assertEquals(param1, "value1");

        final ModuleParam param2 = p.getParam("param2");
        assertEquals(param2.getString("param21"), "value21");
        assertEquals(param2.getString("param22"), "value22");

        final String[] param3 = p.getStrings("param3");
        assertEquals(param3[0], "value3[0]");
        assertEquals(param3[3], "value3[3]");

        final ModuleParam param4 = p.getParam("param4");
        final Long[] param41 = param4.getLongs("param41");
        assertEquals(param41[0], new Long(411));
        assertEquals(param41[1], new Long(412));

        final Double[] param42 = param4.getDoubles("param42");
        assertEquals(param42[0], new Double(42.1D));
        assertEquals(param42[1], new Double(42.2D));

        final String defaultParam = p.getString("defaultParam1");
        assertEquals(defaultParam, "defaultValue1");

        final String overrided = p.getString("defaultParam2");
        assertEquals(overrided, "overrided");
        
        final String userHome = p.getString("user.home");
        assertEquals(userHome, "value: " + System.getProperty("user.home"));

        final String fromGlobal1 = p.getString("from.global1");
        assertEquals(fromGlobal1, "hamed1");

        final String fromGlobal2 = p.getString("from.global2");
        assertEquals(fromGlobal2, "hamed2");
    }

}
