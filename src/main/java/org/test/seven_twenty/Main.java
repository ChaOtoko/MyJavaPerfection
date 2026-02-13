package org.test.seven_twenty;

import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;

import org.test.seven_twenty.utility.in_a_row_counter.LetterInARowCounter;

import java.io.*;
import java.nio.CharBuffer;
import java.util.Arrays;

import static java.lang.System.out;

public final class Main {
    //TODO I should have unit tests for this. Such as checking the args are handled (Especially now that I've expanded them). - Matthew Gavigan - 23/07/2025
    //TODO For errors that occur, rather than returning 1 as the error code, maybe each should have its own code. This is very minor. - Matthew Gavigan - 25/07/2025
    public static void main(final String[] args) {
        final var inputArgDefault = "aaabbccddddddefghhhiiiiihhhxxxxaaaaffffjjjjeeeeeeeeeeeeeeeeePPPPPPaaaaAAAA";
        String inputArg = null;

        String inputFilename = null;

        String outputFilename = null;

        if (args != null && args.length > 0) {
            //TODO In the future the options could be expanded to support setting how many characters in a row are processed as well as the buffer size. - Matthew Gavigan - 25/07/2025
            final int optionText = 't', optionFile = 'f', optionOutputFile = 'o', optionHelp = 'h';

            var cliOptions = new CLOptionDescriptor[]{
                new CLOptionDescriptor(
                    "text",
                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                    optionText,
                    "Pass the input in as a text argument. e.g java main \"aaabbc\""
                ),
                new CLOptionDescriptor(
                    "file",
                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                    optionFile,
                    "Specify the path to a file to use as the input."
                ),
                new CLOptionDescriptor(
                    "output-file",
                    CLOptionDescriptor.ARGUMENT_REQUIRED,
                    optionOutputFile,
                    "Write the output to the specified file."
                ),
                new CLOptionDescriptor(
                    "help",
                    CLOptionDescriptor.ARGUMENT_DISALLOWED,
                    optionHelp,
                    "Display Information about this program and how to use it."
                )
            };
            final var cliArgProcessor = new CLArgsParser(args, cliOptions);

            if (cliArgProcessor.getErrorString() != null) {
                out.println(cliArgProcessor.getErrorString());
                System.exit(1);
            }

            for (final CLOption opt : cliArgProcessor.getArguments()) {
                switch (opt.getId()) {
                    case optionText -> inputArg = opt.getArgument();
                    case optionFile -> inputFilename = opt.getArgument();
                    case optionOutputFile -> outputFilename = opt.getArgument();
                    case optionHelp -> {
                        out.println("Description:");
                        out.println("Accepting input in the form of a collection of letters (both lower and uppercase) the program");
                        out.println("will produce an output in the form of each letter in the same order as appears in the input,");
                        out.println("except it will have attached to it a number indicating the amount of times the same character");
                        out.println("directly follows it.");
                        out.println();
                        out.println("Example:");
                        out.println("Input = \"abbCCC\"");
                        out.println("Output = \"a1b2C3\"");
                        out.println();
                        out.println("Exit Code:");
                        out.println("The program will return 0 upon successful completion and 1 when otherwise.");
                        out.println();
                        out.println("Options:");
                        out.println(CLUtil.describeOptions(cliOptions));
                        out.printf("NOTE: If no input option (-%c or -%c) is specified then a default input will be used.%n", optionText, optionFile);
                        out.printf("This is as follows \"%s\"%n", inputArgDefault);
                        return;
                    }
                }
            }
            if (inputArg != null && inputFilename != null) {
                out.printf(
                    "The program doesn't support setting -%c and -%c at the same time.%n", optionText, optionFile
                );
                System.exit(1);
            }
        }
        if (inputArg == null && inputFilename == null) {
            inputArg = inputArgDefault;
            out.printf("Defaulting input to \"%s\"%n", inputArg);
        }

        if (inputArg != null) {
            final var result = LetterInARowCounter.format(inputArg);

            if (outputFilename == null) {
                out.printf("Result: %s%n", result != null ? result : "<NULL>");
            } else {
                out.printf("Writing to %s...%n", outputFilename);
                try (final var fileWriter = new PrintWriter(outputFilename)) {
                    fileWriter.print(result);
                } catch (FileNotFoundException | SecurityException e) {
                    out.printf("An error occurred accessing %s%n", outputFilename);
                }
                out.printf("Wrote to %s", outputFilename);
            }
        }
        /* Intellij warns about the following "Condition 'inputFilename != null' is always 'true'". I have confirmed
         * this is not the case. */
        else if (inputFilename != null) {
            final long inputFileSize = new File(inputFilename).length();

            final boolean shouldBuffer = inputFileSize > 100_000; // 98 Kilo Bytes

            out.printf("Reading from %s%n", inputFilename);

            PrintWriter fileWriterCreator = null;
            if (outputFilename == null) {
                fileWriterCreator = new PrintWriter(out);
                out.print("Result: ");
            } else {
                out.printf("Writing to %s%n", outputFilename);
                try {
                    //noinspection resource
                    fileWriterCreator = new PrintWriter(outputFilename);
                } catch (FileNotFoundException | SecurityException e) {
                    out.printf("Failed to create/access %s%n", outputFilename);
                    System.exit(1);
                }
            }
            if (fileWriterCreator != null) {
                //try-with-resource requires a final variable. This solves that issue.
                try (final PrintWriter fileWriter = fileWriterCreator) {
                    final int bufferSize = shouldBuffer ? 1_000_000 : (int) inputFileSize;
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
                            if (buffer.limit() > 0) {
                                char[] cutoffs = null;

                                if (shouldBuffer) {
                                    /* The cut-off handling described below could also be applied if the input was
                                     * broken up across Strings to avoid the String size limit. */

                                    /* Cut down the buffer further by finding the 2nd to last character in a row, to
                                     * avoid potentially cutting off the last character in a row, as the next character
                                     * to be read might be the same. */
                                    final int lastPos = buffer.limit() - 1;
                                    if (lastPos > 0) {
                                        final char match = buffer.get(lastPos);
                                        for (int i = lastPos - 1; i > -1; i--) {
                                            if (buffer.get(i) != match) {
                                                final int cutOffsStartPosition = i + 1;
                                                cutoffs = Arrays.copyOfRange(
                                                        buffer.array(),
                                                        cutOffsStartPosition, buffer.limit()
                                                );
                                                buffer.limit(cutOffsStartPosition);
                                                break;
                                            }
                                        }
                                    }
                                }

                                //Reset position to access the buffer's relevant contents. Otherwise argInput = "".
                                buffer.position(0);
                                CharSequence argInput = buffer;
                                if (shouldBuffer) {
                                    while (!argInput.isEmpty()) {
                                        final LetterInARowCounter.FormatBufferedResult result = LetterInARowCounter.formatBuffered(
                                            argInput,
                                            bufferSize
                                        );
                                        if (result != null) {
                                            fileWriter.print(
                                                    !result.processedResult().isEmpty()
                                                        ? result.processedResult() : "<Invalid>"
                                            );
                                            argInput = result.remainingInput();
                                        } else {
                                            fileWriter.print("<NULL>");
                                        }
                                    }

                                    if (cutoffs != null) {
                                        buffer.clear();
                                        buffer.put(cutoffs);
                                    }
                                } else {
                                    final String result = LetterInARowCounter.format(argInput);
                                    if (result != null) {
                                        fileWriter.print(!result.isEmpty() ? result : "<Invalid>");
                                    } else {
                                        fileWriter.print("<NULL>");
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        out.printf("Failed to access %s%n", inputFilename);
                        System.exit(1);
                    }
                }
                if (outputFilename != null) {
                    //Nullify to avoid a potential memory leak.
                    fileWriterCreator = null;

                    out.printf("Wrote to %s", outputFilename);
                }
            } else {
                out.printf("Unexpectedly failed to create/access %s%n", outputFilename);
                System.exit(1);
            }
        }
    }
}