package ir.xweb.util;

import static org.junit.Assert.*;
import org.junit.Test;

public class TestValidator {

    @Test
    public void testValidator1() {
        assertTrue(Validator.validate(Validator.VALIDATOR_EMAIL, "ha.hamed@gmail.com", true));
        assertFalse(Validator.validate(Validator.VALIDATOR_EMAIL, "@gmail.com", true));
        Validator.validate(Validator.VALIDATOR_EMAIL, "hamed@web-presence-in-china.com", true);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testValidator2() {
        Validator.validateOrThrow(Validator.VALIDATOR_EMAIL, "ha.hamedgmail", true);
    }



}
