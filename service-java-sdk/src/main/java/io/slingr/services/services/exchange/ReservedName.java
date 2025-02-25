package io.slingr.services.services.exchange;

/**
 * Reserved names used to exchange information with the application (via Extension Broker app)
 *
 */
public abstract class ReservedName {
    public static final String CONNECT_USER = "connectUser";
    public static final String DISCONNECT_USER = "disconnectUser";
    public static final String USER_CONNECTED = "userConnected";
    public static final String USER_DISCONNECTED = "userDisconnected";
}