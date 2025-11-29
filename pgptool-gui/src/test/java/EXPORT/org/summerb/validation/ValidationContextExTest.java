package EXPORT.org.summerb.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * See for valid and invalid examples https://en.wikipedia.org/wiki/Email_address
 *
 * @author sergeyk
 */
public class ValidationContextExTest {

  /** Examples are taken from https://en.wikipedia.org/wiki/Email_address */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "very.common@example.com",
        "disposable.style.email.with+symbol@example.com",
        "other.email-with-hyphen@example.com",
        "fully-qualified-domain@example.com",
        "user.name+tag+sorting@example.com",
        "x@example.com",
        "example-indeed@strange-example.com",
        "test/test@test.com",
        "example@s.example",
        "\"john..doe\"@example.org",
        "mailhost!username@example.org",
        "user%example.com@example.org",
        "user-@example.org",
        "postmaster@[123.123.123.123]"
      })
  public void testValidateEmailFormat_expectOkForUsualEmail(String email) {
    assertTrue(ValidationContextEx.isValidEmail(email));
  }

  /** Examples are taken from https://en.wikipedia.org/wiki/Email_address */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "Abc.example.com",
        "A@b@c@example.com",
        "a\"b(c)d,e:f;g<h>i[j\\k]l@example.com",
        "just\"not\"right@example.com",
        "this is\"not\\allowed@example.com",
        "this\\ still\\\"not\\\\allowed@example.com",
        "i_like_underscore@but_its_not_allowed_in_this_part.example.com"
      })
  public void testValidateEmailFormat_expectFailForInvalidEmails(String email) {
    assertFalse(ValidationContextEx.isValidEmail(email));
  }
}
