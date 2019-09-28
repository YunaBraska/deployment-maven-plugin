package berlin.yuna.mavendeploy.model;

import org.apache.maven.plugin.logging.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static berlin.yuna.mavendeploy.plugin.PluginSession.unicode;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;

public class Logger {

    private final Log LOG;
    private final DateTimeFormatter formatter;
    private LocalDateTime lastLog = LocalDateTime.now();
    boolean debugEnabled = false;

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
        } else {
            debugEnabled = log.isDebugEnabled();
        }
    }

    public void debug(final Object... format) {
        if (debugEnabled) {
            if (LOG == null) {
                System.out.println("[DEBUG]   " + formatMsg(-1, format));
            } else {
                LOG.debug("   " + formatMsg(-1, format));
            }
        }
    }

    public void info(final Object... format) {
        if (LOG == null) {
            System.out.println("[INFO]    " + formatMsg(-1, format));
        } else {
            LOG.info("    " + formatMsg(-1, format));
        }
    }

    public void warn(final Object... format) {
        if (LOG == null) {
            System.err.println("[WARNING] " + formatMsg(0x26A0, format));
        } else {
            LOG.warn(" " + formatMsg(0x26A0, format));
        }
    }

    public void error(final Object... format) {
        if (LOG == null) {
            System.err.println("[ERROR]   " + formatMsg(0x1F940, format));
        } else {
            LOG.error("   " + formatMsg(0x1F940, format));
        }
    }

    public Log getLog() {
        return LOG;
    }

    public Logger enableDebug(final boolean enable) {
        debugEnabled = enable;
        return this;
    }

    private String formatMsg(final int icon, final Object[] format) {
        final LocalDateTime now = LocalDateTime.now();
        final long diff = lastLog.until(now, SECONDS);
        final String msg = format.length > 1 ? format(String.valueOf(format[0]), Arrays.copyOfRange(format, 1, format.length)) : String.valueOf(format[0]);
        lastLog = now;
        return format(
                "[%s]%s %s", now.format(formatter),
                ((debugEnabled) ? " [" + diff + "s]" : ""),
                (icon != -1 ? unicode(icon) + " " + msg : msg)
        );
    }
}
