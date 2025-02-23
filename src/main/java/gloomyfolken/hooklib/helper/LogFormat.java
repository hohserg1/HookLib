package gloomyfolken.hooklib.helper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormat extends Formatter {
    public static final String format = "[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %2$s]: %3$s %4$s %n";
    private Date dat = new Date();

    @Override
    public String format(LogRecord record) {
        this.dat.setTime(record.getMillis());

        String message = this.formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format(format, this.dat, record.getLevel().getName(), message, throwable);
    }
}
