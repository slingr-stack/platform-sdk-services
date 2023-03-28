package io.slingr.services.framework;

import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.services.Users;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.FunctionRequest;
import io.slingr.services.ws.exchange.WebServiceRequest;
import io.slingr.services.framework.annotations.SlingrService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements utilities for PER USER {@link SlingrService} implementations
 *
 * <p>Created by lefunes on 12/03/18.
 */
public class PerUserModule implements IPerUserService {
    private static final Logger logger = LoggerFactory.getLogger(PerUserModule.class);

    private final BaseModule baseModule;
    protected Users users = null;

    /**
     * Builds the default PER USER service implementation
     *
     * @param baseModule base module
     */
    public PerUserModule(BaseModule baseModule) {
        this.baseModule = baseModule;
        this.baseModule.addSystemLifecycleListener(new DefaultServiceLifecycleListener(){
            @Override
            public void extensionBrokerConfigured() {
                initialize();
            }
        });
    }

    /**
     * Initializes the module after that the Extension Broker API is configured
     */
    private void initialize() {
        this.users = new Users(this.baseModule.userDataStore(), this.baseModule.events(), this.baseModule.properties().isDebug());
        logger.info("PER USER module enabled");
    }

    /**
     * Throws an exception if the users are not ready to be used yet.
     */
    private void errorIfUsersNotConfigured(){
        if(this.users == null) {
            throw ServiceException.permanent(ErrorCode.CLIENT, "User data store is not ready to be used yet. Use it in Service.configured() hook or after its execution.");
        }
    }

    @Override
    public Users users() {
        errorIfUsersNotConfigured();
        return this.users;
    }

    @Override
    public Json defaultExternalConnectUser(WebServiceRequest request) throws ServiceException {
        final String token = request.getHeader("token");
        if (StringUtils.isBlank(token) || !token.equals(this.baseModule.properties().getToken())){
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "Token is not valid").returnCode(400);
        }
        errorIfUsersNotConfigured();
        final String userId = request.getPathVariable("userId");
        return this.users.connect(userId, request.getJsonBody());
    }

    @Override
    public void defaultExternalDisconnectUser(WebServiceRequest request) throws ServiceException {
        final String token = request.getHeader("token");
        if (StringUtils.isBlank(token) || !token.equals(this.baseModule.properties().getToken())){
            throw ServiceException.permanent(ErrorCode.ARGUMENT, "Token is not valid").returnCode(400);
        }
        errorIfUsersNotConfigured();
        final String userId = request.getPathVariable("userId");
        this.users.disconnect(userId);
    }

    @Override
    public Json defaultMethodConnectUsers(FunctionRequest request){
        // gets the user that pushed 'Connect To Service'
        errorIfUsersNotConfigured();
        return this.users.connect(request.getUserId(), request.getJsonParams(), request.getFunctionId());
    }

    @Override
    public void defaultMethodDisconnectUsers(FunctionRequest request){
        // gets the user that pushed 'Disconnect From Service'
        errorIfUsersNotConfigured();
        this.users.disconnect(request.getUserId(), request.getFunctionId());
    }
}
