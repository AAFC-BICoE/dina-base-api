package ca.gc.aafc.dina.translator;

import java.util.regex.Pattern;

/**
 * Translates numbers to letters (alphabetical value for a number) and vice versa.
 *
 */
public final class NumberLetterTranslator {

  /**
   * regex for any character not in alphabet and capitalized.
   */
  private static final Pattern NON_ALPHABET_PATTERN = Pattern.compile("[^A-Z]");

  /**
   * Protect against a potential int overflow
   */
  private static final int MAX_SUPPORTED_LETTERS = 6;

  /** Utility class This class should not be constructed. */
  private NumberLetterTranslator() { }

  /**
   * Generate the alphabetical value for a number, where the letters roll over after reaching Z.
   *
   * Examples: 1 -&gt; A, 26 -&gt; Z, 27 -&gt; AA, 52 -&gt; AZ
   *
   * @param givenNumber the number you want alphabetized, only non-null, non-zero,
   *            positive whole numbers.
   * @return the letter
   * @throws IllegalArgumentException the illegal argument exception
   */
  public static String toLetter(Integer givenNumber) {

    if (givenNumber == null) {
      return null;
    }

    if (givenNumber <= 0) {
      throw new IllegalArgumentException(
          "Does not accept Integers less than zero. Your input : " + givenNumber.toString());
    }

    int number = givenNumber;

    // The following equation calculates the number of letters the character array
    // should have.
    // The formula is for converting to log base 26, as the range each additional
    // character adds
    // is exponential. I.e Integer Range 1-26 returns buf[1], Range 27-702 returns
    // buf[2] and so on.
    char[] buf = new char[(int) Math.floor(Math.log(25L * (number + 1)) / Math.log(26))];

    // for each element in buf, populate it with the correct character
    for (int i = buf.length - 1; i >= 0; i--) {
      number--;
      buf[i] = (char) ('A' + number % 26); // using ASCII with A as the starting point.
      number /= 26;
    }
    return new String(buf);
  }

  /**
   * Gets the number from an alphabetized number, where the letters roll over after reaching Z.
   *
   * Examples: A -&gt; 1, Z -&gt; 26, AA -&gt; 27, AZ -&gt; 52
   *
   * @param givenLetter the alphabetized number, A-Z alphabetical only
   * @return the number as an Integer
   */
  public static Integer toNumber(String givenLetter) {

    // If nothing is given, nothing is given back.
    if (givenLetter == null) {
      return null;
    }

    String letter = givenLetter.toUpperCase();

    if (NON_ALPHABET_PATTERN.matcher(letter).find()) {
      throw new IllegalArgumentException("Alphabetical[A-Z] Inputs only. Your input : " + letter);
    }

    if (letter.length() > MAX_SUPPORTED_LETTERS) {
      throw new IllegalArgumentException(
          "Input should have less than " + MAX_SUPPORTED_LETTERS + " letters. Your input : "
              + letter.length() + " letters");
    }

    int currIntValue = (int) letter.charAt(0) - 64;  // charAt returns the ASCII number and the alphabets start
    int currLetterIdx = 1;
    while(currLetterIdx < letter.length()) {
      currIntValue *= 26; //each iteration is a full alphabet round
      currIntValue += letter.charAt(currLetterIdx) - 64;
      currLetterIdx++;
    }

    return currIntValue;
  }
}
