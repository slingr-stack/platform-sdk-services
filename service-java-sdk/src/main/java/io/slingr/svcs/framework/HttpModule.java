package io.slingr.svcs.framework;

import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.services.HttpService;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.ws.exchange.FunctionRequest;
import io.slingr.svcs.ws.exchange.WebServiceRequest;
import io.slingr.svcs.ws.exchange.WebServiceResponse;
import io.slingr.svcs.framework.annotations.SlingrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements utilities for HTTP {@link SlingrService} implementations
 *
 * <p>Created by lefunes on 12/03/18.
 */
public class HttpModule implements IHttpSvc {
    private static final Logger logger = LoggerFactory.getLogger(HttpModule.class);

    private final IApiUriSource apiUriSource;
    private final BaseModule baseModule;
    private HttpService httpService = null;

    /**
     * Builds the default HTTP service implementation
     *
     * @param apiUriSource source of the API URI
     * @param baseModule base module
     */
    public HttpModule(IApiUriSource apiUriSource, BaseModule baseModule) {
        this.apiUriSource = apiUriSource;
        this.baseModule = baseModule;
        this.baseModule.addSystemLifecycleListener(new DefaultSvcLifecycleListener(){
            @Override
            public void svcStarted() {
                initialize();
            }
        });
    }

    /**
     * Initializes the module after that the service is started
     */
    private void initialize() {
        this.httpService = new HttpService(
                this.apiUriSource.getApiUri(),
                this.baseModule.events(),
                this.baseModule.files(),
                this.baseModule.properties().isDebug()
        );
        logger.info("HTTP module enabled");
    }

    /**
     * Throws an exception if the HTTP service is not ready to be used yet.
     */
    private void errorIfHttpServiceNotConfigured(){
        if(this.httpService == null) {
            throw SvcException.permanent(ErrorCode.CLIENT, "HTTP service is not ready to be used yet. Use it in Svc.svcStarted() hook or after its execution.");
        }
    }

    @Override
    public HttpService httpService() {
        errorIfHttpServiceNotConfigured();
        return this.httpService;
    }

    @Override
    public Json defaultGetRequest(FunctionRequest request) {
        errorIfHttpServiceNotConfigured();
        return this.httpService.defaultGetRequest(request);
    }

    @Override
    public Json defaultPostRequest(FunctionRequest request) {
        errorIfHttpServiceNotConfigured();
        return this.httpService.defaultPostRequest(request);
    }

    @Override
    public Json defaultPutRequest(FunctionRequest request) {
        errorIfHttpServiceNotConfigured();
        return this.httpService.defaultPutRequest(request);
    }

    @Override
    public Json defaultDeleteRequest(FunctionRequest request) {
        errorIfHttpServiceNotConfigured();
        return this.httpService.defaultDeleteRequest(request);
    }

    @Override
    public Json defaultHeadRequest(FunctionRequest request) {
        errorIfHttpServiceNotConfigured();
        return this.httpService.defaultHeadRequest(request);
    }

    @Override
    public Json defaultPatchRequest(FunctionRequest request) {
        errorIfHttpServiceNotConfigured();
        return this.httpService.defaultPatchRequest(request);
    }

    @Override
    public Json defaultOptionsRequest(FunctionRequest request) {
        errorIfHttpServiceNotConfigured();
        return this.httpService.defaultOptionsRequest(request);
    }

    @Override
    public WebServiceResponse defaultWebhookProcessor(WebServiceRequest request) {
        errorIfHttpServiceNotConfigured();
        return this.httpService.defaultWebhookProcessor(request);
    }
}
