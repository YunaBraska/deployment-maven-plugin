package berlin.yuna.mavendeploy.model;

import org.apache.maven.plugin.logging.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;

public class Logger {

    private final Log LOG;
    private final DateTimeFormatter formatter;
    private LocalDateTime lastLog = LocalDateTime.now();

    public Logger() {
        this(null);
    }

    public Logger(final Log log) {
        this(log, null);
    }

    public Logger(final Log log, final String timeFormat) {
        formatter = DateTimeFormatter.ofPattern(timeFormat == null ? "yyyy-MM-dd HH:mm:ss" : timeFormat);
        LOG = log;
        if (LOG == null) {
            warn("Maven logger is not set - fall back to console");
        }
    }

    public void debug(final Object... format) {
        if (LOG == null) {
            System.out.println("[DEBUG]   " + formatMsg(format));
        } else {
            LOG.debug("   " + formatMsg(format));
        }
    }

    public void info(final Object... format) {
        if (LOG == null) {
            System.out.println("[INFO]    " + formatMsg(format));
        } else {
            LOG.info("    " + formatMsg(format));
        }
    }

    public void warn(final Object... format) {
        if (LOG == null) {
            System.err.println("[WARNING] " + formatMsg(format));
        } else {
            LOG.warn(" " + formatMsg(format));
        }
    }

    public void error(final Object... format) {
        if (LOG == null) {
            System.err.println("[ERROR]   " + formatMsg(format));
        } else {
            LOG.error("   " + formatMsg(format));
        }
    }

    public Log getLog() {
        return LOG;
    }

    private String formatMsg(final Object[] format) {
        final LocalDateTime now = LocalDateTime.now();
        final long diff = lastLog.until(now, SECONDS);
        final String msg = format.length > 1 ? format(String.valueOf(format[0]), Arrays.copyOfRange(format, 1, format.length)) : String.valueOf(format[0]);
        lastLog = now;
        return format(
                "[%s]%s %s", now.format(formatter),
                ((LOG != null && LOG.isDebugEnabled()) ? " [" + diff + "s]" : ""),
                msg
        );
    }
}
