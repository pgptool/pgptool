package EXPORT.org.summerb.validation;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.summerb.validation.ValidationContext;
import org.summerb.validation.errors.InvalidEmailValidationError;

public class ValidationContextEx extends ValidationContext {

	public static final Predicate<String> EMAIL_REGEXP = Pattern.compile(
			"(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")
			.asPredicate();

	/**
	 * IMPORTANT !!! See doc of {@link #isValidEmail(String)} method
	 */
	@Override
	public boolean validateEmailFormat(String email, String fieldToken) {
		if (isValidEmail(email)) {
			return true;
		}

		add(new InvalidEmailValidationError(fieldToken));
		return false;
	}

	/**
	 * Checks if email is valid.
	 * 
	 * <p>
	 * ATTENTION: In this implementation we cut several corners in order to simplify
	 * code. We're ignoring several edge cases which seem to be invalid for human
	 * eye, while according to specification these are valid cases.
	 * </p>
	 * 
	 * Method will incorrectly report as <b>invalid</b>, while these are
	 * <b>valid</b> emails:
	 * 
	 * <pre>
	 *  admin@mailserver1
	 *  " "@example.org
	 *  "very.(),:;<>[]\".VERY.\"very@\\ \"very\".unusual"@strange.example.com
	 *  "postmaster@[IPv6:2001:0db8:85a3:0000:0000:8a2e:0370:7334]"
	 * </pre>
	 *
	 * Method will incorrectly report as <b>valid</b>, while this is <b>invalid</b>
	 * email:
	 * 
	 * <pre>
	 *  1234567890123456789012345678901234567890123456789012345678901234+x@example.com
	 * </pre>
	 * 
	 */
	public static boolean isValidEmail(String email) {
		return EMAIL_REGEXP.test(email.toLowerCase());
	}

}
