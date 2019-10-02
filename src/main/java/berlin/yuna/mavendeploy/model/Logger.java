package berlin.yuna.mavendeploy.model;

import org.apache.maven.plugin.logging.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;

import static berlin.yuna.mavendeploy.plugin.PluginSession.hildeSecrets;
import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.codehaus.plexus.logging.Logger.LEVEL_DEBUG;
import static org.codehaus.plexus.logging.Logger.LEVEL_DISABLED;
import static org.codehaus.plexus.logging.Logger.LEVEL_ERROR;
import static org.codehaus.plexus.logging.Logger.LEVEL_FATAL;
import static org.codehaus.plexus.logging.Logger.LEVEL_INFO;
import static org.codehaus.plexus.logging.Logger.LEVEL_WARN;

public class Logger implements Log {

    private final DateTimeFormatter formatter;
    private final HashMap<Integer, String> logTypes = new HashMap<>();
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private int logLevel = 0;
    private LocalDateTime lastLog = LocalDateTime.now();

    public Logger() {
        this(null);
    }

    public Logger(final String timeFormat) {
        logTypes.put(LEVEL_DEBUG, "[" + ANSI_CYAN + "DEBUG" + ANSI_RESET + "]    ");
        logTypes.put(LEVEL_INFO, "[" + ANSI_BLUE + "INFO" + ANSI_RESET + "]     ");
        logTypes.put(LEVEL_WARN, "[" + ANSI_YELLOW + "[WARNING" + ANSI_RESET + "]  ");
        logTypes.put(LEVEL_ERROR, "[" + ANSI_RED + "ERROR" + ANSI_RESET + "]    ");
        logTypes.put(LEVEL_FATAL, "[" + ANSI_PURPLE + "FATAL" + ANSI_RESET + "]    ");
        logTypes.put(LEVEL_DISABLED, "[" + ANSI_GREEN + "DISABLED" + ANSI_RESET + "] ");
        formatter = DateTimeFormatter.ofPattern(timeFormat == null ? "yyyy-MM-dd HH:mm:ss" : timeFormat);
    }

    public void debug(final Object... format) {
        print(LEVEL_DEBUG, -1, null, format);
    }

    public void info(final Object... format) {
        print(LEVEL_INFO, -1, null, format);
    }

    public void warn(final Object... format) {
        print(LEVEL_WARN, 0x26A0, null, format);
    }

    public void error(final Object... format) {
        print(LEVEL_ERROR, 0x1F940, null, format);
    }

    public void fatal(final Object... format) {
        print(LEVEL_FATAL, 0x1F940, null, format);
    }

    private void print(final int logLevel, final int icon, final Throwable throwable, final Object... format) {
        if (this.logLevel <= logLevel && logLevel < LEVEL_DISABLED) {
            final String result = logTypes.get(logLevel)
                    + " [" + LocalDateTime.now().format(formatter) + "]"
                    + ((this.logLevel <= LEVEL_DEBUG) ? " [" + lastLog.until(LocalDateTime.now(), SECONDS) + "s]" : "")
                    + (icon != -1 ? " " + unicode(icon) : "")
                    + " " + formatMsg(format)
                    + " " + toString(throwable);
            if (logLevel == LEVEL_ERROR || logLevel == LEVEL_WARN) {
                System.err.println(result);
            } else {
                System.out.println(result);
            }
        }
        lastLog = LocalDateTime.now();
    }

    private String formatMsg(final Object[] format) {
        final String msg = format.length > 1 ? format(String.valueOf(format[0]), Arrays.copyOfRange(format, 1, format.length)) : String.valueOf(format[0]);
        return hildeSecrets(msg);
    }

    public void debug(final CharSequence content) {
        print(LEVEL_DEBUG, -1, null, content);
    }

    public void debug(final CharSequence content, final Throwable error) {
        print(LEVEL_DEBUG, -1, error, content);
    }

    public void debug(final Throwable error) {
        print(LEVEL_DEBUG, -1, error, "");
    }

    public void info(final CharSequence content) {
        print(LEVEL_INFO, -1, null, content);
    }

    public void info(final CharSequence content, final Throwable error) {
        print(LEVEL_INFO, -1, error, content);
    }

    public void info(final Throwable error) {
        print(LEVEL_INFO, -1, error, "");
    }

    public void warn(final CharSequence content) {
        print(LEVEL_WARN, 0x26A0, null, content);
    }

    public void warn(final CharSequence content, final Throwable error) {
        print(LEVEL_WARN, 0x26A0, error, content);
    }

    public void warn(final Throwable error) {
        print(LEVEL_WARN, 0x26A0, error, "");
    }

    public void error(final CharSequence content) {
        print(LEVEL_ERROR, 0x1F940, null, content);
    }

    public void error(final CharSequence content, final Throwable error) {
        print(LEVEL_ERROR, 0x1F940, error, content);
    }

    public void error(final Throwable error) {
        print(LEVEL_ERROR, 0x1F940, error, "");
    }

    public boolean isDebugEnabled() {
        return logLevel <= LEVEL_DEBUG;
    }

    public boolean isInfoEnabled() {
        return logLevel <= LEVEL_INFO;
    }

    public boolean isWarnEnabled() {
        return logLevel <= LEVEL_WARN;
    }

    public boolean isErrorEnabled() {
        return logLevel <= LEVEL_ERROR;
    }

    public final void setLogLevel(final int logLevel) {
        this.logLevel = logLevel;
    }

    private String toString(final Throwable error) {
        if (error != null) {
            final StringWriter sWriter = new StringWriter();
            error.printStackTrace(new PrintWriter(sWriter));
            return lineSeparator() + sWriter.toString();
        }
        return "";
    }
}
