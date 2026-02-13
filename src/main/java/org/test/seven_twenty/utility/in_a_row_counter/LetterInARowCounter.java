package org.test.seven_twenty.utility.in_a_row_counter;

public final class LetterInARowCounter extends CharacterInARowCounter {
    static {
        charValidator = Character::isLetter;
    }

    //Wrap the super class methods so that this class' static constructor is triggered and overrides charValidator.

    /** See {@link CharacterInARowCounter#format(CharSequence)}**/
    public static String format(final CharSequence input) {
        return CharacterInARowCounter.format(input);
    }

    /** See {@link CharacterInARowCounter#formatBuffered(CharSequence, int)}**/
    public static FormatBufferedResult formatBuffered(final CharSequence input, final int processedAmt) {
        return CharacterInARowCounter.formatBuffered(input, processedAmt);
    }


    private LetterInARowCounter(final char ch) {
        super(ch);
    }
}