package io.slingr.svcs;

import io.slingr.svcs.framework.*;
import io.slingr.svcs.services.HttpService;
import io.slingr.svcs.services.Users;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.ws.exchange.FunctionRequest;
import io.slingr.svcs.ws.exchange.WebServiceRequest;
import io.slingr.svcs.ws.exchange.WebServiceResponse;
import io.slingr.svcs.framework.annotations.SlingrService;

/**
 * Abstract class that must be implemented by all services with support for HTTP services and PER USER functions/events,
 * and annotated with {@link SlingrService}.
 *
 * <p>This contains all the basic support to interact with the Slingr platform, with an external HTTP service and to deal
 * with PER USER functions and events.
 *
 * <p>Created by lefunes on 12/03/18.
 */
public abstract class HttpPerUserSvc extends Svc implements IHttpSvc, IPerUserSvc, IApiUriSource {

    private final PerUserModule perUserModule;
    private final HttpModule httpModule;

    /**
     * Builds the default HTTP PER USER service implementation
     */
    public HttpPerUserSvc() {
        this.perUserModule = new PerUserModule(this.baseModule);
        this.httpModule = new HttpModule(this, this.baseModule);
    }

    @Override
    public abstract String getApiUri();

    @Override
    public Users users() {
        return this.perUserModule.users();
    }

    @Override
    public HttpService httpService() {
        return this.httpModule.httpService();
    }

    @Override
    public final Json defaultExternalConnectUser(WebServiceRequest request) {
        return this.perUserModule.defaultExternalConnectUser(request);
    }

    @Override
    public final void defaultExternalDisconnectUser(WebServiceRequest request) {
        this.perUserModule.defaultExternalDisconnectUser(request);
    }

    @Override
    public final Json defaultMethodConnectUsers(FunctionRequest request) {
        return this.perUserModule.defaultMethodConnectUsers(request);
    }

    @Override
    public final void defaultMethodDisconnectUsers(FunctionRequest request) {
        this.perUserModule.defaultMethodDisconnectUsers(request);
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
