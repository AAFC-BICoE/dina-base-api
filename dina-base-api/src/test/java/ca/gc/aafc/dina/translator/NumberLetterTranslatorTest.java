package ca.gc.aafc.dina.translator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NumberLetterTranslatorTest {

  @Test
  public void getLetter_OnValidIndex_ReturnMatchingLetter() {
    assertEquals("A", NumberLetterTranslator.toLetter(1)); // This function should return
    assertEquals("B", NumberLetterTranslator.toLetter(2)); // The letter representation of
    assertEquals("C", NumberLetterTranslator.toLetter(3)); // The number given. Where the
    assertEquals("Z", NumberLetterTranslator.toLetter(26)); // Order is sequential
    assertEquals("AA", NumberLetterTranslator.toLetter(27), "After Z, AA should follow"); // (A = 1, B = 2, C= 3).
    assertEquals("AB", NumberLetterTranslator.toLetter(28), "The sequential pattern should be the same after adding a digit");
    assertEquals("AAA", NumberLetterTranslator.toLetter(703), "Beginning of triple digits at Int 703");

    // method should not break even when provided a large number
    assertNotNull(NumberLetterTranslator.toLetter(123456969));
  }

  @Test
  public void getLetter_OnInvalidIndex_ReturnNull() {
    assertNull(NumberLetterTranslator.toLetter(null));
  }

  @Test
  public void getLetter_OnInvalidIndex_ThrowException() {
    assertThrows(IllegalArgumentException.class,
        () -> NumberLetterTranslator.toLetter(0));
  }

  @Test
  public void getLetter_OnInvalidIndex2_ThrowException() {
    assertThrows(IllegalArgumentException.class,
        () -> NumberLetterTranslator.toLetter(-1));
  }

  @Test
  public void getNumber_OnValidLetter_ReturnMatchingInteger() {
    assertEquals(1, NumberLetterTranslator.toNumber("A").intValue()); // checking that the equalities are correct
    assertEquals(27, NumberLetterTranslator.toNumber("AA").intValue()); // between numbers and letters
    assertEquals(26, NumberLetterTranslator.toNumber("Z").intValue());
    assertEquals(2, NumberLetterTranslator.toNumber("b"), "Should not be case sensitive");
  }

  @Test
  public void getNumber_OnInvalidLetter_ReturnNull() {
    assertNull(NumberLetterTranslator.toNumber(null));
  }

  @Test
  public void toNumber_toLetterRoundtrip_Equals() {
    int number = NumberLetterTranslator.toNumber("AAAAA");
    assertEquals("AAAAA", NumberLetterTranslator.toLetter(number));
  }

@Test
  public void toNumber_tooManyLetters_ThrowException() {
    assertThrows(IllegalArgumentException.class,
        () -> NumberLetterTranslator.toNumber("AAAAAAA"));
  }

  @Test
  public void getNumber_OnInvalidLetter2_ThrowException() {
    assertThrows(IllegalArgumentException.class,
        () -> NumberLetterTranslator.toNumber("-A")); // Alphabetical inputs only
  }

  @Test
  public void getNumber_OnInvalidLetter3_ThrowException() {
    assertThrows(IllegalArgumentException.class,
      () -> NumberLetterTranslator.toNumber("é")); // Alphabetical inputs only
  }

}
