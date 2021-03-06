package cloud.foundry.cli.crosscutting.logging;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Test for {@link Log}
 */
public class LogTest {
    private static class TestHandler extends Handler {
        private List<LogRecord> records = new ArrayList<>();

        public List<LogRecord> getRecords() {
            return records;
        }

        @Override
        public void publish(LogRecord logRecord) {
            records.add(logRecord);
        }

        @Override
        public void flush() {

        }

        @Override
        public void close() throws SecurityException {

        }
    }

    private static final TestHandler handler = new TestHandler();

    String uniqueTestString = null;

    Log testLog = null;

    @BeforeAll
    private static void setUpAll() {
        Log.addHandler(handler);
    }

    // no idea why, but running the tearDown after each test seems to break all but the first test
    // therefore, we run it once for now
    @AfterAll
    private static void tearDownAll() {
        Log.removeHandler(handler);
    }

    @BeforeEach
    private void setUp() {
        testLog = Log.getLog("cloud.foundry.cli.LogTest");

        assert uniqueTestString == null;
        uniqueTestString = makeRandomTestString();

        // make sure logger is reset to default loglevel
        Log.setDefaultLogLevel();

        // check common preconditions
        assertNoMessagesRecorded();
    }

    @AfterEach
    private void tearDown() {
        testLog = null;

        uniqueTestString = null;

        // make sure logger is reset to default loglevel
        Log.setDefaultLogLevel();

        // check common postconditions
        // all messages should've been consumed
        assertNoMessagesRecorded();
    }

    private static String makeRandomTestString() {
        // from '0' to 'z'
        int leftLimit = 0x30;
        int rightLimit = 0x7a;
        int length = 32;

        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private static LogRecord popLastRecord() {
        List<LogRecord> records = handler.getRecords();
        return records.remove(records.size() - 1);
    }

    private static void assertNoMessagesRecorded() {
        assert handler.getRecords().size() == 0;
    }

    @Test
    public void testError() {
        testLog.error(uniqueTestString);

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getLevel() == Log.ERROR_LEVEL;
        assert lastRecord.getMessage().contains(uniqueTestString);

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testWarning() {
        testLog.warning(uniqueTestString);

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getLevel() == Log.WARNING_LEVEL;
        assert lastRecord.getMessage().contains(uniqueTestString);

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testInfo() {
        testLog.info(uniqueTestString);

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getLevel() == Log.INFO_LEVEL;
        assert lastRecord.getMessage().contains(uniqueTestString);

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testVerbose() {
        testLog.verbose(uniqueTestString);

        // verbose messages should not be visible by default
        assertNoMessagesRecorded();

        // but they should be visible in verbose mode
        Log.setVerboseLogLevel();

        testLog.verbose(uniqueTestString);

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getLevel() == Log.VERBOSE_LEVEL;
        assert lastRecord.getMessage().contains(uniqueTestString);

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testDebug() {
        testLog.debug(uniqueTestString);

        // debug messages should not be visible by default
        assertNoMessagesRecorded();

        // but they should be visible in verbose mode
        Log.setDebugLogLevel();

        testLog.debug(uniqueTestString);

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getLevel() == Log.DEBUG_LEVEL;
        assert lastRecord.getMessage().contains(uniqueTestString);

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testException() {
        Throwable exception = null;

        // generate exception
        try {
            int i = 1 / 0;
        } catch (ArithmeticException e) {
            exception = e;
            testLog.exception(e, uniqueTestString);
        }

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getLevel() == Log.ERROR_LEVEL;
        assert lastRecord.getMessage().contains(uniqueTestString);
        assert lastRecord.getThrown() == exception;

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testQuietMode() {
        // test whether quiet mode works as intended
        Log.setQuietLogLevel();

        // none of these messages should reach our handler
        testLog.debug(uniqueTestString);
        testLog.verbose(uniqueTestString);
        testLog.info(uniqueTestString);
        testLog.warning(uniqueTestString);

        // debug messages should not be visible by default
        assertNoMessagesRecorded();

        // errors should be recorded even in quiet mode
        testLog.error(uniqueTestString);

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getLevel() == Log.ERROR_LEVEL;
        assert lastRecord.getMessage().contains(uniqueTestString);

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testErrorWithMultipleArguments() {
        testLog.error("aaa", "bbb", "ccc", "ddd");

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getMessage().contains("aaa");
        assert lastRecord.getMessage().contains("bbb");
        assert lastRecord.getMessage().contains("ccc");
        assert lastRecord.getMessage().contains("ddd");

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testWarningWithMultipleArguments() {
        testLog.warning("aaa", "bbb", "ccc", "ddd");

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getMessage().contains("aaa");
        assert lastRecord.getMessage().contains("bbb");
        assert lastRecord.getMessage().contains("ccc");
        assert lastRecord.getMessage().contains("ddd");

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testInfoWithMultipleArguments() {
        testLog.info("aaa", "bbb", "ccc", "ddd");

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getMessage().contains("aaa");
        assert lastRecord.getMessage().contains("bbb");
        assert lastRecord.getMessage().contains("ccc");
        assert lastRecord.getMessage().contains("ddd");

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testVerboseWithMultipleArguments() {
        Log.setVerboseLogLevel();

        testLog.verbose("aaa", "bbb", "ccc", "ddd");

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getMessage().contains("aaa");
        assert lastRecord.getMessage().contains("bbb");
        assert lastRecord.getMessage().contains("ccc");
        assert lastRecord.getMessage().contains("ddd");

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testDebugWithMultipleArguments() {
        Log.setDebugLogLevel();

        testLog.debug("aaa", "bbb", "ccc", "ddd");

        LogRecord lastRecord = popLastRecord();
        assert lastRecord.getMessage().contains("aaa");
        assert lastRecord.getMessage().contains("bbb");
        assert lastRecord.getMessage().contains("ccc");
        assert lastRecord.getMessage().contains("ddd");

        // now all messages should be handled
        assertNoMessagesRecorded();
    }

    @Test
    public void testLogFactoryMethodWithClassOutsideNativePackage() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Log.getLog(Test.class)
        );
    }

}
