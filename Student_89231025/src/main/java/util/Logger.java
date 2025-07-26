package util;

import java.text.SimpleDateFormat;

public class Logger {
    public static final String RESET = "\u001B[0m";
    public static final String ERROR = "\u001B[31m";
    public static final String WARN = "\u001B[32m";
    public static final String INFO = "\u001B[33m";
    public static final String SUCCESS = "\u001B[34m";
    public static final String DEBUG = "\u001B[35m";
    public static final String STATUS = "\u001B[36m";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static void log(String msg, LogLevel level) {
        String threadName = Thread.currentThread().getName();
        String timestamp = dateFormat.format(System.currentTimeMillis());
        String messagePrefix = "[" + timestamp + "][" + threadName + "] " + level + ": ";

        // changed cause for some reason I HAD TO SWITCH FROM JDK AND SDK 21 TO JDK AND SDK 8
       /* switch (level) {
            case Info -> messagePrefix = INFO + messagePrefix + RESET;
            case Warn -> messagePrefix = WARN + messagePrefix + RESET;
            case Error -> messagePrefix = ERROR + messagePrefix + RESET;
            case Debug -> messagePrefix = DEBUG + messagePrefix + RESET;
            case Status -> messagePrefix = STATUS + messagePrefix + RESET;
            case Success -> messagePrefix = SUCCESS + messagePrefix + RESET;
        }*/

        switch (level) {
            case Info:
                messagePrefix = INFO + messagePrefix + RESET;
                break;
            case Warn:
                messagePrefix = WARN + messagePrefix + RESET;
                break;
            case Error:
                messagePrefix = ERROR + messagePrefix + RESET;
                break;
            case Debug:
                messagePrefix = DEBUG + messagePrefix + RESET;
                break;
            case Status:
                messagePrefix = STATUS + messagePrefix + RESET;
                break;
            case Success:
                messagePrefix = SUCCESS + messagePrefix + RESET;
                break;
        }


        System.out.println(messagePrefix  + msg);
    }

    public static void log(String msg) {
        log(msg, LogLevel.Info);
    }
}
