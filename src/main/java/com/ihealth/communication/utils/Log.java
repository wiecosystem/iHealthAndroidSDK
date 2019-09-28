package com.ihealth.communication.utils;


/**
 * Created by jing on 16/9/27.
 */

public class Log {

    private static  final  String TAG = "Log";
    private Log() {
    }

    public enum Level {
        VERBOSE(2),
        DEBUG(3),
        INFO(4),
        WARN(5),
        ERROR(6);

        int level = 2;
        Level(int level) {
            this.level = level;
        }
    }


    /**
     * Send a VERBOSE log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void v(String tag, String msg) {
         Logger.v(tag, msg);
    }

    /**
     * Send a DEBUG log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void d(String tag, String msg) {
         Logger.d(tag, msg);
    }

    /**
     * Send an INFO log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void i(String tag, String msg) {
         Logger.i(tag, msg);
    }

    /**
     * Send a WARN log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void w(String tag, String msg) {
         Logger.w(tag, msg);
    }

    /**
     * Send an ERROR log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static void e(String tag, String msg) {
         Logger.e(tag, msg);
    }


    /**
     * Print method information
     * @param tag
     * @param methodName
     * @param parameters
     */
    public static void p(String tag, Level level, String methodName, Object... parameters) {
        if (methodName == null) {
            return;
        }
        try {
            if (methodName.contains("()")) {
                methodName = methodName.replace("()", "");
            }

            String info;

            if (parameters.length == 0) {
                info = methodName + "()";
            } else {
                StringBuilder builder = new StringBuilder(methodName + "(");
                for (Object parameter : parameters) {
                    builder.append(parameter).append(", ");
                }
                builder.delete(builder.length() - 2, builder.length()).append(")");
                info = builder.toString();
            }

            switch(level) {
                case VERBOSE:
                    Logger.v(tag, info);
                    break;
                case DEBUG:
                    Logger.d(tag, info);
                    break;
                case INFO:
                    Logger.i(tag, info);
                    break;
                case WARN:
                    Logger.w(tag, info);
                    break;
                case ERROR:
                    Logger.e(tag, info);
                    break;
            }

        } catch (Exception e) {
            Logger.w(TAG, "printMethodInfo -- Exception:" + e);
        }
    }
}
