package org.test.seven_twenty.utility.in_a_row_counter;

import java.util.Calendar;

import static java.lang.System.out;

final class TestTimeLogger {
    private static final String timeLogFormat = "%s|%tT.%tL - %s...|%n";
    static void printCurrentTime(final String eventDescription, final boolean shouldOutputLeadingNewLine) {
        final var now = Calendar.getInstance();
        out.printf(timeLogFormat, shouldOutputLeadingNewLine ? "\n" : "", now, now, eventDescription);
    }

    /** This makes sure that regardless of the reason, when a test stops the time is logged. So that one can get an
     * idea of how long it ran for. This is because Intellij does not display the elapsed time for manually terminated
     * tests.**/
    static void setupTestTerminateTimeLog() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(
                        () -> {
                            printCurrentTime("Terminated Test", true);
                            Runtime.getRuntime().halt(89);
                        }
                )
        );
    }
}