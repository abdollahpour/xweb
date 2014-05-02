package ir.xweb.util;

public class Validator {

	public final static String VALIDATOR_ALL      = "";

    public final static String VALIDATOR_NAME     = "^[A-Za-z0-9 ]{3,20}$";

    /**
     * This regex does not support all the possible validation. For complete email detection you can use Apache commons-validator project.
     * @see <a href="http://commons.apache.org/proper/commons-validator">commons-validator</a>
     */
    public final static String VALIDATOR_EMAIL    = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";

	//public final static String VALIDATOR_EMAIL    = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    public final static String VALIDATOR_USERNAME = "^[A-Za-z0-9_]{4,20}$";

    public final static String VALIDATOR_PASSWORD = "^[A-Za-z0-9!@#$%^&*()_]{6,20}$";

    public final static String VALIDATOR_GENDER   = "1|2";		// match number = ^\d*$

    public final static String VALIDATOR_AGE      = "^[A-Za-z0-9!@#$%^&*()_]{6,20}$";

    public final static String VALIDATOR_JOB      = "^[A-Za-z0-9!@#$%^&*()_]{6,20}$";

    public final static String VALIDATOR_ASSOCIATION = "^[A-Za-z0-9!@#$%^&*()_]{6,20}$";

	public final static String VALIDATOR_NUMBER   = "^[A-Za-z0-9!@#$%^&*()_]{6,20}$";

	public static boolean validate(final String regex, final String text, final boolean required) {
		if(regex == null) {
			throw new IllegalArgumentException("null regex");
		}
		
		if(text == null) {
			return !required;
		} else if(regex.length() == 0) {
			return true;
		} else {
			return text.matches(regex);
		}
	}

    public static void validateOrThrow(final String regex, final String text, final boolean required) {
        if(!validate(regex, text, required)) {
            throw new IllegalArgumentException("Illegal value: " + text);
        }
    }

}
