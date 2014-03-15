package ir.xweb.module;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

public class TestEmptyModule extends TestModule {

    public TestEmptyModule() throws IOException {
    }

    @Test
    public void testValidation() {
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("param1", "value1");
        map.put("param2", "value2");

        final EmptyModule emptyModule = getManager().getModuleOrThrow(EmptyModule.class);
        final boolean result1 = emptyModule.getRoleManager().hasPermission(map, "user");
        Assert.assertTrue(result1);

        final boolean result2 = emptyModule.getRoleManager().hasPermission(map, "admin");
        Assert.assertTrue(result2);
    }

}
