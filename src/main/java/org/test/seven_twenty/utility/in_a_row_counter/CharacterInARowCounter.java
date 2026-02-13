package org.test.seven_twenty.utility.in_a_row_counter;

import java.util.LinkedList;

public sealed class CharacterInARowCounter permits LetterInARowCounter {
    /** This is used by {@link #formatBuffered(CharSequence, int)} to determine if the current character in its input
     *  argument is valid and should be processed. As it is protected child classes of CharacterInARowCounter can be
     *  created to narrow down valid character such as only letters being valid. **/
    protected static CharacterInARowValidator charValidator = (final char ch) -> true;

    /** This is a helper method to {@link #formatBuffered(CharSequence, int)}, the whole input will be processed.
     *
     * @param input See the referenced method.<br>
     * @return As the whole input is processed there won't be any remaining unprocessed input and so the processed
     * output is returned.
     */
    public static String format(final CharSequence input) {
        final FormatBufferedResult result = formatBuffered(input, 0);
        return result != null ? result.processedResult.toString() : null;
    }

    /** Using the valid characters in input this basically will produce a basic output in the form of each valid
     *  character in the same order as appears in the input, except it will have attached to it a number indicating the
     *  amount of times the same character directly follows it. An invalid characters will not be included.<br>
     *  <br>
     *  <b>Example:</b><br>
     *  <i>Valid characters = a-z and A-Z</i>
     *  <i>Input = "ab!!bCCC"</i><br>
     *  <i>Basic Output = "a1b2C3"</i>
     *
     * @param input A collection of characters to process. THis is a CharSequence as not only does String implement it,
     *              but also CharBuffer which can be used to wrap a char array in.<br>
     *
     * @param processCharAmount The amount of valid characters that should be processed. A value of 0 indicates
     *                          unlimited. This exists because the basic output generated can be larger than the input
     *                          and as such may exceed Java memory limits.
     * <br>
     * @return A Record containing the output of the processed input and the unprocessed remaining input. Which can then
     *         be fed back into the method to be processed.**/
    public static FormatBufferedResult formatBuffered(final CharSequence input, final int processCharAmount) {
        FormatBufferedResult result = null;

        if(input != null && !input.isEmpty() && processCharAmount > -1) {
            //TODO I may create my own simple LinkedList. This is very minor - Matthew Gavigan - 22/07/2025
            final var processedInput = new LinkedList<CharacterInARowCounter>();

            CharacterInARowCounter currentCharToProcess = null;
            int currentInputPosition = 0;
            for(; currentInputPosition < input.length(); currentInputPosition++) {
                final char currentInputChar = input.charAt(currentInputPosition);
                if(charValidator.isValid(currentInputChar)) {
                    if(currentCharToProcess == null || currentCharToProcess.tracked != currentInputChar) {
                        if(processCharAmount == 0 || processedInput.size() < processCharAmount) {
                            currentCharToProcess = new CharacterInARowCounter(currentInputChar);
                            processedInput.add(currentCharToProcess);
                        } else {
                            break;
                        }
                    } else {
                        currentCharToProcess.increment();
                    }
                } else {
                    currentCharToProcess = null;
                }
            }

            final var resultBuilder = new StringBuilder();

            //noinspection SimplifyStreamApiCallChains
            processedInput.stream().forEachOrdered(resultBuilder::append);

            result = new FormatBufferedResult(
                resultBuilder.toString(),
                input.subSequence(currentInputPosition, input.length())
            );
        }

        return result;
    }


    /** This is used to store the return of {@link #formatBuffered(CharSequence, int)}.<br>
     *
     *  @param processedResult The main result of the method above.<br>
     *  @param remainingInput The remaining unprocessed input of the method above's input
     *  parameter.**/
    public record FormatBufferedResult(CharSequence processedResult, CharSequence remainingInput) { }


    /* Whilst I could pull the data structure out, I feel it is best that it is not accessible outside this class
     * compared with count being accessible inside the class.*/

    /** This stores the character currently having its directly trailing occurrences tracked.**/
    private final char tracked;

    /** This stores the running count a character appears in a row in a collection of characters.<br>
     * <br>
     * <b>NOTE:</b> <i>Whilst it can be accessed directly by the static method format. One must not and must use the
     * increment method to affect its value.</i>*/
    private short countConsecutive;

    protected CharacterInARowCounter(final char tracked) {
        this.tracked = tracked;
        increment();
    }

    /** Handles incrementing {@link #countConsecutive}.
     *  @throws ArithmeticException if count were to overflow. */
    private void increment() {
        countConsecutive++;
        if(countConsecutive < 0) {
            //TODO At some point implement a localised string, extremely minor - Matthew Gavigan - 23/07/2025
            throw new ArithmeticException("Short overflow");
        }
    }

    /** Used to display the current amount of occurrences of the {@link #tracked} character.**/
    @Override
    public String toString() {
        return String.format("%s%d", tracked, countConsecutive);
    }
}