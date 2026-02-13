package org.test.seven_twenty.utility.in_a_row_counter;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.test.seven_twenty.utility.in_a_row_counter.TestDirHandler.setupTestDir;
import static org.test.seven_twenty.utility.in_a_row_counter.TestTimeLogger.printCurrentTime;
import static org.test.seven_twenty.utility.in_a_row_counter.TestTimeLogger.setupTestTerminateTimeLog;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class LetterInARowCounterTest {
    /** This method demonstrates how to handle buffered formatting and is used by related tests.
     * @param fileWriter Where the processedResult of the below method is written to.
     * @See {@link CharacterInARowCounter#formatBuffered(CharSequence, int)}**/
    private static void handleFormatBuffered(
        CharSequence argInput,
        final int argProcessedAmt,
        final PrintWriter fileWriter
    ) {
        while (!argInput.isEmpty()) {
            Exception caughtException = null;
            LetterInARowCounter.FormatBufferedResult result = null;
            try {
                result = LetterInARowCounter.formatBuffered(argInput, argProcessedAmt);
            } catch (Exception ex) {
                caughtException = ex;
            }

            //Assert
            assertNull(caughtException);
            fileWriter.print(result.processedResult());
            argInput = result.remainingInput();
        }
    }


    @SuppressWarnings("unused")
    final static List<Arguments> testFormatInputArgs = Arrays.asList(
        argumentSet("Positive", "abbcccDDDD", "a1b2c3D4"),
        argumentSet("Negative - null", null, null),
        argumentSet("Negative - empty", "", null),
        argumentSet("Negative - invalid character at start", "!aabbbcccc", "a2b3c4"),
        argumentSet("Negative - invalid character at end", "abbccc!!!!", "a1b2c3"),
        argumentSet("Negative - invalid character in the middle", "ab!b", "a1b1b1"),
        argumentSet("Negative - all invalid character", "!!!!!!!!!!", "")
    );
    @Order(1)
    @ParameterizedTest
    @FieldSource("testFormatInputArgs")
    void testFormatInput(final String arg, final String expectedResult) {
        //Act
        Exception caughtException = null;
        String result = null;
        try {
            result = LetterInARowCounter.format(arg);
        } catch(NullPointerException ex) {
            caughtException = ex;
        }

        //Assert
        assertNull(caughtException);
        assertEquals(expectedResult, result);
    }

    @Order(2)
    @Test
    void testFormatInputFile() throws IOException {
        //Arrange
        setupTestTerminateTimeLog();

        final String testName = new Object() { }.getClass().getEnclosingMethod().getName();

        final String inputFilename = String.format("%s-Input.txt", testName);
        final int bufferSize = 100_000;

        //Generate argInput
        final String textArgInput = "argInput";
        out.printf("Generating %s...%n", textArgInput);
        try (final var fileWriter = new PrintWriter(inputFilename)) {
            for (int i = 0; i < bufferSize; i++) {
                fileWriter.print(i % 2 == 0 ? 'a' : 'b');
            }
        }
        out.printf("Generated %s%n", textArgInput);

        final File testDir = setupTestDir();
        final var outputFile = new File(testDir, String.format("%s-Output.txt", testName));

        //Act
        final String methodName = "format";
        printCurrentTime(String.format("Running %s", methodName), false);
        try (final var fileWriter = new PrintWriter(outputFile)) {
            //I presume BufferedReader will manage FileReader.
            try (final var fileReader = new BufferedReader(new FileReader(inputFilename), bufferSize)) {
                final CharBuffer buffer = CharBuffer.allocate(bufferSize);
                int amtRead = 0;
                while (amtRead > -1) {
                    amtRead = fileReader.read(buffer);

                    /* Cut how much of the buffer is accessible. So that its garbage elements aren't treated as
                     * possible characters to process.*/
                    buffer.limit(buffer.position());

                    //If amtRead = -1 and there is no more of the buffer to process then skip to end.
                    if(buffer.limit() > 0) {
                        //Reset position to access the buffer's relevant contents. Otherwise argInput = "".
                        buffer.position(0);
                        //Act
                        Exception caughtException = null;
                        String result = null;
                        try {
                            result = LetterInARowCounter.format(buffer);
                        } catch(NullPointerException ex) {
                            caughtException = ex;
                        }
                        assertNull(caughtException);
                        fileWriter.print(result);
                    }
                }
            }
        }
        printCurrentTime(String.format("Ran %s", methodName), false);
        out.println();

        assertTrue(outputFile.exists());
        assertTrue(outputFile.isFile());
        assertTrue(outputFile.length() > new File(inputFilename).length());

        //Assassinate

        // As it's a test, and if the file is not deleted the file gets overwritten. I don't care if delete fails.
        //noinspection ResultOfMethodCallIgnored
        new File(inputFilename).delete();

        //Leave the output file as one may want to analyse it afterwards.
    }

    @Order(3)
    @Test
    void testFormatOverflow() {
        //Arrange
        final StringBuilder arg = new StringBuilder();
        for(int i = 0; i < Short.MAX_VALUE + 1; i++) {
            arg.append('a');
        }

        //Act
        Exception caughtException = null;
        try {
            LetterInARowCounter.format(arg);
        } catch(ArithmeticException ex) {
            caughtException = ex;
        }

        //Assert
        assertNotNull(caughtException);
    }

    /* From working with the testFormatOverflow test. I realised that the processedResult could potentially overflow
     * so this test tests that. It is from here that I changed the original format method of CharacterInARowCounter
     * into formatBuffered. Followed by then relegating format into to a helper method that call formatBuffered.*/
    @Disabled(value = "TEST SKIPPED:A crash is occurring somewhere.")
    @Order(4)
    @Test
    void testFormatResultOverflow() {
        //Arrange
        setupTestTerminateTimeLog();

        final String textArg = "arg";
        out.printf("Generating %s...%n", textArg);
        final StringBuilder arg = new StringBuilder();
        for(int i = 0; i < Integer.MAX_VALUE / 2; i++) {
            arg.append(i % 2 == 0 ? 'a' : 'b');
        }
        out.printf("Generated %s%n", textArg);

        //Act
        final String textMethod = "LetterInARowCounter.format()";
        printCurrentTime(String.format("Running %s", textMethod), false);
        Exception caughtException = null;
        try {
            LetterInARowCounter.format(arg);
        } catch(Exception ex) {
            caughtException = ex;
        }
        out.printf("Ran %s%n", textMethod);

        //Assert
        assertNotNull(caughtException);
    }

    @SuppressWarnings("unused")
    final static List<Arguments> testFormatBufferedArgs = Arrays.asList(
        argumentSet("Valid argProcessedAmt", 1),
        argumentSet("Invalid argProcessedAmt", -1)
    );
    /** This tests and demonstrates how one can use formatBuffered to write to the console.**/
    @Order(5)
    @ParameterizedTest
    @FieldSource("testFormatBufferedArgs")
    void testFormatBuffered(final int argProcessedAmt) {
        //Arrange
        CharSequence argInput = "aaabbbbccccccc";

        final CharSequence[] expectedProcessedResult, expectedRemainingInput;
        if(argProcessedAmt == 1) {
            expectedProcessedResult = new CharSequence[] {
                "a3",
                "b4",
                "c7"
            };
            expectedRemainingInput = new CharSequence[] {
                "bbbbccccccc",
                "ccccccc",
                ""
            };
        } else {
            expectedProcessedResult = null;
            expectedRemainingInput = null;
        }

        if(argProcessedAmt == 1) {
            final String printPrefix = "Processed result = ";
            out.print(printPrefix);
            for (byte b = 0; b < (byte) 3; b++) {
                //Act
                Exception caughtException = null;
                LetterInARowCounter.FormatBufferedResult result = null;
                try {
                    result = LetterInARowCounter.formatBuffered(argInput, argProcessedAmt);
                } catch (Exception ex) {
                    caughtException = ex;
                }

                //Assert
                assertNull(caughtException);
                assertNotNull(result);

                assertEquals(expectedProcessedResult[b], result.processedResult());

                out.print(result.processedResult()); //This shows how you could handle output

                assertEquals(expectedRemainingInput[b], result.remainingInput());
                argInput = result.remainingInput();
            }
            out.println();
        } else {
            //Act
            Exception caughtException = null;
            LetterInARowCounter.FormatBufferedResult result = null;
            try {
                result = LetterInARowCounter.formatBuffered(argInput, argProcessedAmt);
            } catch (Exception ex) {
                caughtException = ex;
            }

            //Assert
            assertNull(caughtException);
            assertNull(result);
        }
    }


    //TODO For the tests below, In the future expand into concurrency, if viable, to improve processing times. - Matthew Gavigan - 24/07/2025

    @SuppressWarnings("unused")
    final static List<Arguments> testFormatBufferedOverflowInputStringArgs = Arrays.asList(
        argumentSet("Basic", 10, 1)

        /* Note: This is a long running test. On my oct-core it took close to 10 minutes to finish. So you may want to
         * comment it out, though this is the main test. */
        //argumentSet("Int Size Result", Integer.MAX_VALUE / 2, 1_000_000)
    );
    @Order(6)
    @ParameterizedTest
    @FieldSource("testFormatBufferedOverflowInputStringArgs")
    void testFormatBufferedOverflowInputString(
        final int inputStringSize,
        final int argProcessedAmt
    ) throws FileNotFoundException {
        //Arrange
        setupTestTerminateTimeLog();

        //Generate argInput
        final String textArgInput = "argInput";
        out.printf("Generating %s...%n", textArgInput);
        final StringBuilder argBuilder = new StringBuilder();
        for (int i = 0; i < inputStringSize; i++) {
            argBuilder.append(i % 2 == 0 ? 'a' : 'b');
        }
        out.printf("Generated %s...%n", textArgInput);

        CharSequence argInput = argBuilder.toString();

        final File testDir = setupTestDir();
        final var outputFile = new File(
            testDir,
            String.format("%s-Output.txt", new Object() { }.getClass().getEnclosingMethod().getName())
        );

        //Act
        printCurrentTime("Running formatBuffered", false);
        try (final var fileWriter = new PrintWriter(outputFile)) {
        //Act & Assert======================================================
            handleFormatBuffered(argInput, argProcessedAmt, fileWriter);
        } catch (FileNotFoundException ex) {
            out.println("Failed to create output file.");
        }
        printCurrentTime("Ran formatBuffered", false);
        out.println();

        assertTrue(outputFile.exists());
        assertTrue(outputFile.isFile());
        assertTrue(outputFile.length() > argBuilder.length());
    }

    /* If inputFileContents is a numeric String then that is converted to a number and the input file is generated to
     * that size. */
    @SuppressWarnings("unused")
    final static List<Arguments> testFormatBufferedOverflowInputFileArgs = Arrays.asList(
        argumentSet("No Cutoff", "aaa", 0, 5),
        argumentSet("Cutoff", "abbabbabba", 1, 5),
        argumentSet("1 million", String.valueOf(1_000_000), 1_000_000, 1_000_000)

        /* Note: This is a long running test. On my oct-core it took close to 10 minutes to finish.
         * So you may want to comment it out */
        //argumentSet("Int size", String.valueOf(Integer.MAX_VALUE / 2), 1_000_000, 1_000_000)

        /* READ ME: This is a very long running test. On my oct-core it took close to half an hour to finish.
         * So you may want to comment it out, though this is the main test. To test that formatBuffered can handle over
         * int.max.
        argumentSet("4GB", String.valueOf((long) Integer.MAX_VALUE * 2), 1_000_000, 1_000_000)*/
    );
    /** This demonstrates how one could read from a file and tests that functionality. **/
    @Order(7)
    @ParameterizedTest
    @FieldSource("testFormatBufferedOverflowInputFileArgs")
    void testFormatBufferedOverflowInputFile(
        final String inputFileContents,
        final int argProcessedAmt,
        final int bufferSize
    ) throws IOException {
        //Arrange
        setupTestTerminateTimeLog();

        final String testName = new Object() { }.getClass().getEnclosingMethod().getName();

        final String inputFilename = String.format("%s-Input.txt", testName);

        //Generate argInput
        final String textArgInput = "argInput";
        out.printf("Generating %s...%n", textArgInput);
        try (final var fileWriter = new PrintWriter(inputFilename)) {
            Long parsed = null;
            try {
                parsed = Long.valueOf(inputFileContents);
            } catch(NumberFormatException ignored) { }

            if(parsed != null) {
                for (long l = 0; l < parsed; l++) {
                    fileWriter.print(l % 2 == 0 ? 'a' : 'b');
                }
            } else {
                fileWriter.print(inputFileContents);
            }
        }
        out.printf("Generated %s%n", textArgInput);

        final File testDir = setupTestDir();
        final var outputFile = new File(testDir, String.format("%s-Output.txt", testName));

        //Act
        printCurrentTime("Running formatBuffered", false);
        try (final var fileWriter = new PrintWriter(outputFile)) {
            //I presume BufferedReader will manage FileReader.
            try (final var fileReader = new BufferedReader(new FileReader(inputFilename), bufferSize)) {
                final CharBuffer buffer = CharBuffer.allocate(bufferSize);
                int amtRead = 0;

                /* If any cut-off characters (described below) have been rewritten back to the buffer then its
                 * position will be above 0, and the cut-off characters will need processing regardless of if
                 * there is any more file contents to be read. */
                while (amtRead > -1 || buffer.position() > 0) {
                    amtRead = fileReader.read(buffer);

                     /* Cut how much of the buffer is accessible. So that its garbage elements aren't treated as
                      * possible characters to process.*/
                    buffer.limit(buffer.position());

                    //If amtRead = -1 and there is no more of the buffer to process then skip to end.
                    if(buffer.limit() > 0) {
                         /* Cut down the buffer further by finding the 2nd to last character in a row, to avoid
                          * potentially cutting off the last character in a row, as the next character to be read might
                          * be the same. */
                        char[] cutoffs = null;
                        final int lastPos = buffer.limit() - 1;
                        if (lastPos > 0) {
                            final char match = buffer.get(lastPos);
                            for (int i = lastPos - 1; i > -1; i--) {
                                if (buffer.get(i) != match) {
                                    final int cutOffsStartPosition = i + 1;
                                    cutoffs = Arrays.copyOfRange(buffer.array(), cutOffsStartPosition, buffer.limit());
                                    buffer.limit(cutOffsStartPosition);
                                    break;
                                }
                            }
                        }

                        //Reset position to access the buffer's relevant contents. Otherwise argInput = "".
                        buffer.position(0);
        //Act & Assert==============================================================================
                        handleFormatBuffered(buffer, argProcessedAmt, fileWriter);

                        if (cutoffs != null) {
                            buffer.clear();
                            buffer.put(cutoffs);
                        }
                    }
                }
            }
        }
        printCurrentTime("Ran formatBuffered", false);
        out.println();

        assertTrue(outputFile.exists());
        assertTrue(outputFile.isFile());

        if(!inputFileContents.equals(testFormatBufferedOverflowInputFileArgs.get(0).get()[0])) {
            assertTrue(outputFile.length() > new File(inputFilename).length());
        }

        //Assassinate

         // As it's a test, and if the file is not deleted the file gets overwritten. I don't care if delete fails.
        //noinspection ResultOfMethodCallIgnored
        //new File(inputFilename).delete(); //TODO Uncomment.

        //Leave the output file as one may want to analyse it afterwards.
    }
}