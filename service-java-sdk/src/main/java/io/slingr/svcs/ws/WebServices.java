package io.slingr.svcs.ws;

import io.slingr.svcs.configurations.Configuration;
import io.slingr.svcs.configurations.SvcsProperties;
import io.slingr.svcs.configurations.sources.EnvVarsSource;
import io.slingr.svcs.configurations.sources.PropertySource;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Web Services server that exposes the URIs to exchange information from the platform (via Extension Broker ) and
 * from external services.
 *
 * <p>Created by lefunes on 28/03/18.
 */
public class WebServices {
    private static final Logger logger = LoggerFactory.getLogger(WebServices.class);

    // timeouts
    private static final int TIMEOUT_10_MINUTES = 10 * 60 * 1000; // 10 minutes

    private final Server server = new Server();
    private final WebServicesProcessor webServicesProcessor;
    private final AtomicBoolean status = new AtomicBoolean(false);
    private final ReentrantLock statusLock = new ReentrantLock();
    private final static List<PropertySource> envVarsSource = Arrays.asList(new EnvVarsSource());
    // ssl config properties
    private final static String USE_SSL_PROPERTY = "USE_SSL";
    private final static String SSL_KEYSTORE_PATH_PROPERTY = "SSL_KEYSTORE_PATH";
    private final static String SSL_PASS_PROPERTY = "SSL_PASS";


    /**
     * Initialize the Web Service instance
     *
     * @param properties service current configuration
     * @param webServicesProcessor processor of the server requests
     */
    public WebServices(SvcsProperties properties, WebServicesProcessor webServicesProcessor) {
        final ServerConnector connector;
        boolean useSsl = !properties.isLocalDeployment() || useSsl();
        logger.info(String.format("Use SSL connection: [%s]", useSsl));
        if (useSsl) {
            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());
            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setKeyStorePath(getKeystorePath());
            sslContextFactory.setKeyStorePassword(getKeystorePass());
            sslContextFactory.setKeyManagerPassword(getKeystorePass());
            connector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
        } else {
            connector = new ServerConnector(server);
        }
        connector.setPort(properties.getWebServicesPort());
        connector.setHost("0.0.0.0");
        connector.setIdleTimeout(TIMEOUT_10_MINUTES);
        connector.setStopTimeout(TIMEOUT_10_MINUTES);
        connector.setSoLingerTime(TIMEOUT_10_MINUTES);

        server.setConnectors(new Connector[]{connector});
        server.setHandler(webServicesProcessor);

        this.webServicesProcessor = webServicesProcessor;
    }

    /**
     * Starts the web services server
     */
    public void start(){
        statusLock.lock();
        logger.info("Starting Web Services server...");
        try {
            if (status.get()){
                // service is already started
                logger.warn("Web Services is already started.");
            } else {
                // service is stopped

                // start the server
                server.start();
                status.set(true);
                logger.info("Web Services started");
            }
        } catch(Exception ex) {
            logger.error(String.format("Exception when start Web Services: %s", ex.getMessage()), ex);
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Stops the web services server
     */
    public void stop(){
        statusLock.lock();
        logger.info("Stopping Web Services server...");
        try {
            if (status.get()) {
                // service is started

                // stop the server
                server.setStopTimeout(-1);
                server.stop();
                status.set(false);
                logger.info("Web Services stopped");
            } else {
                // service is already stopped
                logger.warn("Web Services is already stopped.");
            }
        } catch (Exception ex) {
            logger.error(String.format("Exception when stop Web Services: %s", ex.getMessage()), ex);
        } finally {
            statusLock.unlock();
        }
    }

    /**
     * Setup the parameters to deal with exceptions
     *
     * @param maxRedelivers maximum retries of a request that throws an exception
     */
    public void setupDefaultExceptionsProperties(int maxRedelivers){
        this.webServicesProcessor.setupDefaultExceptionsProperties(maxRedelivers);
    }

    /**
     * Setup the parameters to deal with permanent exceptions
     *
     * @param maxRedelivers maximum retries of a request that throws a permanent exception
     * @param delay delay between retries
     */
    public void setupPermanentExceptionsProperties(int maxRedelivers, long delay){
        this.webServicesProcessor.setupPermanentExceptionsProperties(maxRedelivers, delay);
    }

    /**
     * Setup the parameters to deal with retryable exceptions
     *
     * @param maxRedelivers maximum retries of a request that throws a retryable exception
     * @param delay delay between retries
     */
    public void setupRetryableExceptionsProperties(int maxRedelivers, long delay){
        this.webServicesProcessor.setupRetryableExceptionsProperties(maxRedelivers, delay);
    }

    // helpers

    private static String getKeystorePath() {
        return Configuration.resolveProperty(envVarsSource, SSL_KEYSTORE_PATH_PROPERTY);
    }

    private static String getKeystorePass() {
        return Configuration.resolveProperty(envVarsSource, SSL_PASS_PROPERTY);
    }

    private static boolean useSsl() {
        return Configuration.resolveBooleanProperty(envVarsSource, USE_SSL_PROPERTY);
    }

}
