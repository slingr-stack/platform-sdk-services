package io.slingr.svcs.services.logs;

import org.apache.commons.lang3.StringUtils;

/**
 * App log level (equivalent ot com.idea2.db.entities.LogLevel class on common/core library)
 *
 * <p>Created by lefunes on 23/12/15.
 */
public enum AppLogLevel {
    INFO,
    WARN,
    ERROR;

    /**
     * Returns the AppLogLevel enum constant with the specified level. The string must match exactly an identifier used
     * to declare an AppLogLevel enum constant. A null value is returned if this enum type has no constant with the
     * specified level name.
     *
     * @param level value to check
     * @return the AppLogLevel enum constant with the specified level, or null if this enum type has no constant with
     * the specified level name.
     */
    public static AppLogLevel fromString(String level) {
        if(StringUtils.isNotBlank(level)) {
            try {
                return valueOf(level.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the AppLogLevel enum constant with the specified level. The string must match exactly an identifier used
     * to declare an AppLogLevel enum constant. INFO value is returned if this enum type has no constant with the
     * specified level name.
     *
     * @param level value to check
     * @return the string of the specified level, or INFO if this enum type has no constant with the specified name
     */
    public static String checkStringValue(String level) {
        AppLogLevel val = fromString(level);
        if(val == null){
            val = INFO;
        }
        return val.name();
    }
}
