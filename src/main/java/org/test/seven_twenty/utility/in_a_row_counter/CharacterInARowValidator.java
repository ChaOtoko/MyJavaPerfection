package org.test.seven_twenty.utility.in_a_row_counter;

interface CharacterInARowValidator {
    /** Will determine if ch is considered valid.
     *  @param ch Character to validate.
     *  @return the processedResult of validating ch.*/
    boolean isValid(final char ch);
}