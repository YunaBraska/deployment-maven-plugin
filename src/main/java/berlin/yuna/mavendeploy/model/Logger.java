package berlin.yuna.mavendeploy.model;

import org.apache.maven.plugin.logging.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;

import static berlin.yuna.mavendeploy.model.Logger.LogLevel.DEBUG;
import static berlin.yuna.mavendeploy.model.Logger.LogLevel.DISABLED;
import static berlin.yuna.mavendeploy.model.Logger.LogLevel.ERROR;
import static berlin.yuna.mavendeploy.model.Logger.LogLevel.FATAL;
import static berlin.yuna.mavendeploy.model.Logger.LogLevel.INFO;
import static berlin.yuna.mavendeploy.model.Logger.LogLevel.WARN;
import static berlin.yuna.mavendeploy.plugin.PluginSession.hideSecrets;
import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static berlin.yuna.mavendeploy.util.MojoUtil.isPresent;
import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.time.temporal.ChronoUnit.SECONDS;

public class Logger implements Log {

    private Consumer<String> consumer;
    private final DateTimeFormatter formatter;
    private final HashMap<LogLevel, String> logTypes = new HashMap<>();
    private LogLevel logLevel;
    private LocalDateTime lastLog = LocalDateTime.now();
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    public Logger() {
        this(null);
    }

    public Logger(final String timeFormat) {
        logTypes.put(DEBUG, "[" + ANSI_CYAN + "DEBUG" + ANSI_RESET + "]    ");
        logTypes.put(INFO, "[" + ANSI_BLUE + "INFO" + ANSI_RESET + "]     ");
        logTypes.put(WARN, "[" + ANSI_YELLOW + "WARNING" + ANSI_RESET + "]  ");
        logTypes.put(ERROR, "[" + ANSI_RED + "ERROR" + ANSI_RESET + "]    ");
        logTypes.put(FATAL, "[" + ANSI_PURPLE + "FATAL" + ANSI_RESET + "]    ");
        logTypes.put(DISABLED, "[" + ANSI_GREEN + "DISABLED" + ANSI_RESET + "] ");
        formatter = DateTimeFormatter.ofPattern(timeFormat == null ? "yyyy-MM-dd HH:mm:ss" : timeFormat);
        setLogLevel((isPresent(System.getenv("DEBUG")) && parseBoolean(System.getenv("DEBUG")))? DEBUG : INFO);
    }

    public void debug(final Object... format) {
        print(DEBUG, -1, null, format);
    }

    public void info(final Object... format) {
        print(INFO, -1, null, format);
    }

    public void warn(final Object... format) {
        print(WARN, 0x26A0, null, format);
    }

    public void error(final Object... format) {
        print(ERROR, 0x1F940, null, format);
    }

    public void fatal(final Object... format) {
        print(FATAL, 0x1F940, null, format);
    }

    private synchronized void print(final LogLevel logLevel, final int icon, final Throwable throwable, final Object... format) {
        if (this.logLevel.ordinal() <= logLevel.ordinal() && logLevel.ordinal() < DISABLED.ordinal()) {
            final String result = logTypes.get(logLevel)
                    + " [" + ANSI_YELLOW + LocalDateTime.now().format(formatter) + ANSI_RESET + "]"
                    + ((this.logLevel.ordinal() <= DEBUG.ordinal()) ? " [" + ANSI_PURPLE + lastLog.until(LocalDateTime.now(), SECONDS) + "s" + ANSI_RESET + "]" : "")
                    + (icon != -1 ? " " + unicode(icon) : "")
                    + " " + formatMsg(format)
                    + " " + toString(throwable);
            print(logLevel, result);
        }
        lastLog = LocalDateTime.now();
    }

    private void print(final LogLevel logLevel, final String result) {
        if (consumer != null) {
            consumer.accept(result);
        } else if (logLevel == ERROR || logLevel == FATAL) {
            System.err.println(result);
        } else {
            System.out.println(result);
        }
    }

    private String formatMsg(final Object[] format) {
        final String msg = format.length > 1 ? format(String.valueOf(format[0]), Arrays.copyOfRange(format, 1, format.length)) : String.valueOf(format[0]);
        return hideSecrets(msg);
    }

    public void debug(final CharSequence content) {
        print(DEBUG, -1, null, content);
    }

    public void debug(final CharSequence content, final Throwable error) {
        print(DEBUG, -1, error, content);
    }

    public void debug(final Throwable error) {
        print(DEBUG, -1, error, "");
    }

    public void info(final CharSequence content) {
        print(INFO, -1, null, content);
    }

    public void info(final CharSequence content, final Throwable error) {
        print(INFO, -1, error, content);
    }

    public void info(final Throwable error) {
        print(INFO, -1, error, "");
    }

    public void warn(final CharSequence content) {
        print(WARN, 0x26A0, null, content);
    }

    public void warn(final CharSequence content, final Throwable error) {
        print(WARN, 0x26A0, error, content);
    }

    public void warn(final Throwable error) {
        print(WARN, 0x26A0, error, "");
    }

    public void error(final CharSequence content) {
        print(ERROR, 0x1F940, null, content);
    }

    public void error(final CharSequence content, final Throwable error) {
        print(ERROR, 0x1F940, error, content);
    }

    public void error(final Throwable error) {
        print(ERROR, 0x1F940, error, "");
    }

    public boolean isDebugEnabled() {
        return logLevel.ordinal() <= DEBUG.ordinal();
    }

    public boolean isInfoEnabled() {
        return logLevel.ordinal() <= INFO.ordinal();
    }

    public boolean isWarnEnabled() {
        return logLevel.ordinal() <= WARN.ordinal();
    }

    public boolean isErrorEnabled() {
        return logLevel.ordinal() <= ERROR.ordinal();
    }

    public Logger setLogLevel(final LogLevel logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public void setConsumer(final Consumer<String> consumer) {
        this.consumer = consumer;
    }

    private String toString(final Throwable error) {
        if (error != null) {
            final StringWriter sWriter = new StringWriter();
            error.printStackTrace(new PrintWriter(sWriter));
            return lineSeparator() + sWriter.toString();
        }
        return "";
    }

    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        FATAL,
        DISABLED;
    }
}
