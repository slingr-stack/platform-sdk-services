package io.slingr.services.configurations;

import io.slingr.services.utils.ThreadAttributes;
import org.apache.commons.lang.StringUtils;

public class ServiceContext {

    public static void initContext(String app, String env) {
        if (!StringUtils.isBlank(app)) {
            setCurrentApp(app);
        }
        if (!StringUtils.isBlank(env)) {
            setCurrentEnv(env);
        }
    }

    public static String getCurrentApp() {
        return (String) ThreadAttributes.get("app");
    }

    public static void setCurrentApp(String app) {
        ThreadAttributes.set("app", app);
    }

    public static void setCurrentEnv(String env) {
        ThreadAttributes.set("env", env);
    }

    public static String getCurrentEnv() {
        return (String) ThreadAttributes.get("env");
    }

    public static String getCurrentProvision() {
        if (getCurrentApp() != null && getCurrentEnv() != null) {
            return getCurrentApp() + "-" + getCurrentEnv();
        }
        return null;
    }

    public static void endContext() {
        setCurrentApp(null);
        setCurrentEnv(null);
    }
}