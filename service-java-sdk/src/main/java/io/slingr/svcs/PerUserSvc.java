package io.slingr.svcs;

import io.slingr.svcs.framework.PerUserModule;
import io.slingr.svcs.framework.IPerUserSvc;
import io.slingr.svcs.services.Users;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.ws.exchange.FunctionRequest;
import io.slingr.svcs.ws.exchange.WebServiceRequest;
import io.slingr.svcs.framework.annotations.SlingrService;

/**
 * Abstract class that must be implemented by all services with support for PER USER functions/events and annotated
 * with {@link SlingrService}.
 *
 * <p>This contains all the basic support to interact with the Slingr platform and to deal with PER USER functions and
 * events.
 *
 * <p>Created by lefunes on 12/03/18.
 */
public abstract class PerUserSvc extends Svc implements IPerUserSvc {

    private final PerUserModule perUserModule;

    /**
     * Builds the default PER USER service implementation
     */
    public PerUserSvc() {
        this.perUserModule = new PerUserModule(this.baseModule);
    }

    @Override
    public Users users() {
        return this.perUserModule.users();
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
}
