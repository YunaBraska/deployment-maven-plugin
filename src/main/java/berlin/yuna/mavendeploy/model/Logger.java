package berlin.yuna.mavendeploy.model;

import org.apache.maven.plugin.logging.Log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;

public class Logger {

    private final Log LOG;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime lastLog = LocalDateTime.now();

    public Logger(final Log log) {
        LOG = log;
        if(LOG == null){
            error("Logger is null - fall back to console");
        }
    }

    public void debug(final Object... format) {
        if (LOG == null) {
            System.out.println(formatMsg(format) + " [DEBUG]   ");
        } else {
            LOG.debug("   " + formatMsg(format));
        }
    }

    public void info(final Object... format) {
        if (LOG == null) {
            System.out.println(formatMsg(format) + " [INFO]    ");
        } else {
            LOG.info("    " + formatMsg(format));
        }
    }

    public void warn(final Object... format) {
        if (LOG == null) {
            System.err.println(formatMsg(format) + " [WARNING] ");
        } else {
            LOG.warn(" " + formatMsg(format));
        }
    }

    public void error(final Object... format) {
        if (LOG == null) {
            System.err.println(formatMsg(format) + " [ERROR]   ");
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
        final String msg = format.length > 1 ? format((String) format[0], (Object[]) Arrays.copyOfRange(format, 1, format.length)) : (String) format[0];
        lastLog = now;
        return format("[%s]%s %s", now.format(formatter), LOG.isDebugEnabled() ? " [" + diff + "s]" : "", msg);
    }
}
