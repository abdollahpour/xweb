package ir.xweb.test.util;

import ir.xweb.util.Validator;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestValidator {

    @Test
    public void testValidator1() {
        assertTrue(Validator.validate(Validator.VALIDATOR_EMAIL, "ha.hamed@gmail.com", true));
        assertFalse(Validator.validate(Validator.VALIDATOR_EMAIL, "@gmail.com", true));
        Validator.validate(Validator.VALIDATOR_EMAIL, "hi@some-mail-server.com", true);
    }

    /*@Test(expected=IllegalArgumentException.class)*/
    public void testValidator2() {
        Validator.validateOrThrow(Validator.VALIDATOR_EMAIL, "ha.hamedgmail", true);
    }



}
