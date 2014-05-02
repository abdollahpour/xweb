/**
 * xweb project
 * Created by Hamed Abdollahpour
 * http://www.mobile4use.com/xweb
 */

package ir.xweb.test.util;


import ir.xweb.util.Tools;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestTools {

    @Test
    public void testGetValidFilename() {
        assertEquals(Tools.isValidFilename("123.txt"), true);
        assertEquals(Tools.isValidFilename("_adb.txt"), true);
        assertEquals(Tools.isValidFilename("_ad/b.txt"), false);
        assertEquals(Tools.isValidFilename("_ad\\b.txt"), false);
        assertEquals(Tools.isValidFilename("_a?d?b.txt"), false);
        assertEquals(Tools.isValidFilename("_ad*b.txt"), false);

    }

}
