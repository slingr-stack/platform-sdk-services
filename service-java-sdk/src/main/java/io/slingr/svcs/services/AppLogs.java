package io.slingr.svcs.services;

import io.slingr.svcs.Svc;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.services.logs.AppLogLevel;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.converters.JsonSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logger that sends the App Logs to the app developer of the application that is using the service
 *
 * <p>Created by lefunes on 12/05/15.
 */
public class AppLogs {
    private static final Logger logger = LoggerFactory.getLogger(AppLogs.class);

    private static final int MAX_LAST_ERRORS_NOTIFIED = 5;
    private final ExtensionBrokerApi api;
    private final boolean debug;
    private int lastErrors = 0;

    /**
     * Constructor only can be called by Extension Broker class
     *
     * @param api extension broker implementation
     * @param debug true if the service shows information useful for debug
     */
    public AppLogs(ExtensionBrokerApi api, boolean debug) {
        this.api = api;
        this.debug = debug;
    }

    /**
     * Sends an INFO app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     */
    public void info(String message){
        info(message, null, null);
    }

    /**
     * Sends an INFO app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error.
     */
    public void info(String message, Json additionalInfo){
        info(message, additionalInfo, null);
    }

    /**
     * Sends an INFO app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     * @param throwable Throwable exception used as complementary information related to the message.
     */
    public void info(String message, Throwable throwable){
        info(message, null, throwable);
    }

    /**
     * Sends an INFO app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error.
     * @param throwable Throwable exception used as complementary information related to the message.
     */
    public void info(String message, Json additionalInfo, Throwable throwable){
        sendAppLog(AppLogLevel.INFO, message, additionalInfo, throwable);
    }

    /**
     * Sends a WARN app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     */
    public void warn(String message){
        warn(message, null, null);
    }

    /**
     * Sends an WARN app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error.
     */
    public void warn(String message, Json additionalInfo){
        warn(message, additionalInfo, null);
    }

    /**
     * Sends a WARN app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     * @param throwable Throwable exception used as complementary information related to the message.
     */
    public void warn(String message, Throwable throwable){
        warn(message, null, throwable);
    }

    /**
     * Sends an WARN app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error.
     * @param throwable Throwable exception used as complementary information related to the message.
     */
    public void warn(String message, Json additionalInfo, Throwable throwable){
        sendAppLog(AppLogLevel.WARN, message, additionalInfo, throwable);
    }

    /**
     * Sends an ERROR app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     */
    public void error(String message){
        error(message, null, null);
    }

    /**
     * Sends an ERROR app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error.
     */
    public void error(String message, Json additionalInfo){
        error(message, additionalInfo, null);
    }

    /**
     * Sends an ERROR app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     * @param throwable Throwable exception used as complementary information related to the message.
     */
    public void error(String message, Throwable throwable){
        error(message, null, throwable);
    }

    /**
     * Sends an ERROR app log to the application in order to show the message to the app developer.
     *
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error.
     * @param throwable Throwable exception used as complementary information related to the message.
     */
    public void error(String message, Json additionalInfo, Throwable throwable){
        sendAppLog(AppLogLevel.ERROR, message, additionalInfo, throwable);
    }

    /**
     * Sends an app log to the application in order to show the message to the app developer.
     *
     * @param level Level of the app log.
     * @param message Message to show as app log to the developer.
     */
    public void sendAppLog(AppLogLevel level, String message){
        sendAppLog(level, message, null, null);
    }

    /**
     * Sends an app log to the application in order to show the message to the app developer.
     *
     * @param level Level of the app log.
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error.
     */
    public void sendAppLog(AppLogLevel level, String message, Json additionalInfo){
        sendAppLog(level, message, additionalInfo, null);
    }

    /**
     * Sends an app log to the application in order to show the message to the app developer.
     *
     * @param level Level of the app log.
     * @param message Message to show as app log to the developer.
     * @param throwable Throwable exception used as complementary information related to the message.
     */
    public void sendAppLog(AppLogLevel level, String message, Throwable throwable){
        sendAppLog(level, message, null, throwable);
    }

    /**
     * Sends an app log to the application in order to show the message to the app developer.
     *
     * @param level Level of the app log.
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error.
     * @param throwable Throwable exception used as complementary information related to the message.
     */
    public void sendAppLog(AppLogLevel level, String message, Json additionalInfo, Throwable throwable){
        sendAppLog(System.currentTimeMillis(), level, message, additionalInfo, throwable);
    }

    /**
     * Sends an app log to the application in order to show the message to the app developer.
     *
     * @param date Timestamp that represents the moment when the app log was generated. It takes the value of the
     *             milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param level Level of the app log.
     * @param message Message to show as app log to the developer.
     */
    public void sendAppLog(Long date, AppLogLevel level, String message){
        sendAppLog(date, level, message, null);
    }

    /**
     * Sends an app log to the application in order to show the message to the app developer.
     *
     * @param date Timestamp that represents the moment when the app log was generated. It takes the value of the
     *             milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param level Level of the app log.
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error
     */
    public void sendAppLog(Long date, AppLogLevel level, String message, Json additionalInfo){
        sendAppLog(date, level, message, additionalInfo, null);
    }

    /**
     * Sends an app log to the application in order to show the message to the app developer.
     *
     * @param date Timestamp that represents the moment when the app log was generated. It takes the value of the
     *             milliseconds from '01/01/1970 12:00 AM'. Per example 1465928711524 is '06/14/2016 6:25:11 PM'
     * @param level Level of the app log.
     * @param message Message to show as app log to the developer.
     * @param additionalInfo json that permits to include information related to the error
     * @param throwable Throwable exception used as complementary information related to the message.
     */
    public void sendAppLog(Long date, AppLogLevel level, String message, Json additionalInfo, Throwable throwable){
        if(debug) {
            logger.info(String.format("%s Sending app log to application", Svc.DEBUG));
        }

        boolean sent = false;
        try {
            additionalInfo = additionalInfo != null ? additionalInfo : Json.map();
            if(throwable != null && !additionalInfo.contains("exception")){
                additionalInfo.setIfNotEmpty("exception", parseCause(throwable, 0));
            }

            api.newAppLogs(date, level.name(), message, additionalInfo);
            sent = true;
            lastErrors = 0;
        } catch (SvcException ex) {
            lastErrors++;
            if (lastErrors < MAX_LAST_ERRORS_NOTIFIED) {
                logger.warn(String.format("Error when try to send an App Log: [%s]", ex.toJson(false)));
            }
        } catch (Exception ex) {
            lastErrors++;
            if (lastErrors < MAX_LAST_ERRORS_NOTIFIED) {
                logger.warn(String.format("Error when try to send an App Log: [%s]", ex instanceof JsonSource ? ((JsonSource) ex).toJson() : ex.getMessage()));
            }
        }

        switch (level){
            case ERROR:
                logger.warn(String.format("App Log: %s%s", message, sent ? "" : " [it was no sent to platform]"), throwable);
                break;
            case WARN:
                logger.warn(String.format("App Log: %s%s", message, sent ? "" : " [it was no sent to platform]"), throwable);
                break;
            case INFO:
                logger.info(String.format("App Log: %s%s", message, sent ? "" : " [it was no sent to platform]"));
                break;
            default:
                logger.warn(String.format("App Log: [Invalid level: %s] %s%s", level, message, sent ? "" : " [it was no sent to platform]"), throwable);
                break;
        }
    }

    /**
     * Tries to convert a Throwable exception in a Json instance
     *
     * @param throwable Throwable exception
     * @return compatible Json object
     */
    private Json parseCause(Throwable throwable, long internalLevel) {
        Json cause = null;
        if(throwable != null) {
            cause = Json.map();
            if(throwable instanceof SvcException){
                cause.setIfNotEmpty("detail", ((SvcException) throwable).toJson(false));
            } else {
                cause.setIfNotEmpty("detail", throwable.getMessage());
            }
            final Json stack = Json.list();
            for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
                stack.push(stackTraceElement.toString());
            }
            cause.setIfNotEmpty("stacktrace", stack);
            if(internalLevel < 10) {
                cause.setIfNotEmpty("cause", parseCause(throwable.getCause(), internalLevel+1));
            }
        }
        return cause;
    }
}
