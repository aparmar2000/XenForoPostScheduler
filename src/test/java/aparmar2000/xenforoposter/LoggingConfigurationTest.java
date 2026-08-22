package aparmar2000.xenforoposter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

class LoggingConfigurationTest {

	@Test
	@DisplayName("Active test runtime logger should use console-only logging without file appender")
	void testActiveTestRuntimeLoggerIsConsoleOnly() {
		Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		assertNotNull(rootLogger.getAppender("CONSOLE"), "Active test logger must output to CONSOLE");
		assertNull(rootLogger.getAppender("FILE"), "Active test logger must NOT have a FILE appender active");
	}

}
