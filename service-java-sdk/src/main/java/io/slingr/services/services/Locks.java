package io.slingr.services.services;

import io.slingr.services.Service;
import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all messages related to locks exchanged with the Extension Broker
 *
 * <p>Created by lefunes on 20/03/18.
 */
public class Locks {
    private static final Logger logger = LoggerFactory.getLogger(Locks.class);

    private final ExtensionBrokerApi api;
    private final boolean debug;

    /**
     * Constructor only can be called by Extension Broker  class
     *
     * @param api extension broker implementation
     * @param debug true if the service shows information useful for debug
     */
    public Locks(ExtensionBrokerApi api, boolean debug) {
        this.api = api;
        this.debug = debug;
    }

    /**
     * Tries to acquire the lock for the specified key.
     *
     * <p>This method is used when a service needs to perform a lock over a key in order to synchronize processes
     * between service instances.
     *
     * @param key Key to be used to perform a lock.
     * @return true if the lock was acquired
     */
    public boolean lock(String key) {
        ExtensionBroker.isNotBlank(key, "empty key to lock");

        if(debug) {
            logger.info(String.format("%s locking key [%s]", Service.DEBUG, key));
        }
        boolean result = false;
        try {
            final Json response = api.acquireLock(key);
            if (response != null && response.contains(Parameter.LOCK_ACQUIRED)) {
                result = response.is(Parameter.LOCK_ACQUIRED);
            }
            if(debug) {
                logger.info(String.format("%s locked key [%s]: %s", Service.DEBUG, key, result));
            }
        } catch (Exception ex) {
            logger.warn(String.format("Exception when try to lock key [%s]", key), ex);
        }
        return result;
    }

    /**
     * Releases the lock for the specified key.
     *
     * <p>This method is used when a service needs to unlock a key in order to permit to other synchronized service
     * instances to lock it.
     *
     * @param key Key to be used to perform an unlock.
     * @return true if the lock was released
     */
    public boolean unlock(String key) {
        ExtensionBroker.isNotBlank(key, "empty key to unlock");

        if(debug) {
            logger.info(String.format("%s unlocking key [%s]", Service.DEBUG, key));
        }
        boolean result = false;
        try {
            final Json response = api.releaseLock(key);
            if (response != null && response.contains(Parameter.LOCK_RELEASED)) {
                result = response.bool(Parameter.LOCK_RELEASED);
            }
            if(debug) {
                logger.info(String.format("%s unlocked key [%s]: %s", Service.DEBUG, key, result));
            }
        } catch (Exception ex) {
            logger.warn(String.format("Exception when try to unlock key [%s]", key), ex);
        }
        return result;
    }

}