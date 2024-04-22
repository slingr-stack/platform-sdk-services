package io.slingr.services;

import io.slingr.services.framework.HttpModule;
import io.slingr.services.framework.IApiUriSource;
import io.slingr.services.framework.IHttpService;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.FunctionRequest;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.ws.exchange.WebServiceResponse;
import io.slingr.services.framework.annotations.SlingrService;

/**
 * Abstract class that must be implemented by all services with support for HTTP services and annotated with
 * {@link SlingrService}.
 *
 * <p>This contains all the basic support to interact with the Slingr platform and with an external HTTP service.
 *
 */
public abstract class HttpService extends Service implements IHttpService, IApiUriSource {

    private final HttpModule httpModule;

    /**
     * Builds the default HTTP service implementation
     */
    public HttpService() {
        this.httpModule = new HttpModule(this, this.baseModule);
    }

    @Override
    public abstract String getApiUri();

    @Override
    public io.slingr.services.services.HttpService httpService() {
        return this.httpModule.httpService();
    }

    @Override
    public final Json defaultGetRequest(FunctionRequest request) {
        return this.httpModule.defaultGetRequest(request);
    }

    @Override
    public final Json defaultPostRequest(FunctionRequest request) {
        return this.httpModule.defaultPostRequest(request);
    }

    @Override
    public final Json defaultPutRequest(FunctionRequest request) {
        return this.httpModule.defaultPutRequest(request);
    }

    @Override
    public final Json defaultDeleteRequest(FunctionRequest request) {
        return this.httpModule.defaultDeleteRequest(request);
    }

    @Override
    public final Json defaultHeadRequest(FunctionRequest request) {
        return this.httpModule.defaultHeadRequest(request);
    }

    @Override
    public final Json defaultPatchRequest(FunctionRequest request) {
        return this.httpModule.defaultPatchRequest(request);
    }

    @Override
    public final Json defaultOptionsRequest(FunctionRequest request) {
        return this.httpModule.defaultOptionsRequest(request);
    }

    @Override
    public final WebServiceResponse defaultWebhookProcessor(WebServiceRequest request) {
        return this.httpModule.defaultWebhookProcessor(request);
    }
}
