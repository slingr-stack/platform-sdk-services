//////////////////
// Dependencies //
//////////////////
//We load env vars from dotenv if we are in local deployment
//The file loaded will depend on the NODE_ENV var.If not defined, it dafaults to .env
if (process.env._local_deployment === undefined) {
    let envPath = process.env.NODE_ENV === undefined ? './.env' : `./.${process.env.NODE_ENV}.env`;
    require('dotenv').config({ path: envPath}); 
}
const
    util = require('util'),
    fs = require('fs'),
    http = require('http'),
    https = require('https'),
    axios = require('axios'),
    compression = require('compression'),
    express = require('express'),
    bodyParser = require('body-parser');

//The instance of the service.
let svc = {
    settings: null,
    hooks: {
        onSvcServicesConfigured: null,
        onConfigurationReady: null,
        onWebServicesReady: null,
        onSvcStart: null,
        onSvcStop: null
    },
    functions: {},
    webServices: {},
    dataStores: null,
    logger: null,
    appLogger: null,
    events: null,
    files: null,
    scripts: null,
    users: null,
    application: null,
    locks: null,
    httpModule: {}
};
let settings,
    svcConfig,
    definitions,
    logger,
    appLogger,
    events,
    files,
    dataStores,
    scripts,
    users,
    application,
    locks,
    httpModule;

///////////////////////
// Svc Services //
///////////////////////
const loadSvcServices = async () => {
    logger = require('./services/loggers/logs');
    appLogger = require('./services/loggers/appLogs');
    events = require('./services/events/events');
    files = require('./services/files/files');
    dataStores = require('./services/dataStores/dataStores');
    scripts = require('./services/scripts/scripts');
    users = require('./services/users/users');
    application = require('./services/application/application');
    locks = require('./services/locks/locks');

    httpModule = require('./services/httpModule/httpModule');

    settings.isUsingProxy = await require('./services/settings/settings').checkProxyStatus();

    svc.logger = logger;
    svc.appLogger = appLogger;
    svc.events = events;
    svc.files = files;
    svc.dataStores = dataStores;
    svc.scripts = scripts;
    svc.users = users;
    svc.application = application;
    svc.locks = locks;
    svc.httpModule = axios;
}

const convertException = (err, code) => {
    if (!err) {
        return {
            __svc_exception__: true,
            message: 'There is an issue on the service',
            error: !code ? { code: 'general', name: 'General exception' } : code
        }
    } else {
        if (typeof err === 'string') {
            return {
                __svc_exception__: true,
                message: err,
                error: !code ? { code: 'general', name: 'General exception' } : code
            }
        } else if (err.__svc_exception__) {
            return err
        } else if (err.message) {
            return {
                __svc_exception__: true,
                message: err.message,
                additionalInfo: err,
                error: !code ? { code: 'general', name: 'General exception' } : code
            }
        } else {
            return {
                __svc_exception__: true,
                message: 'There is an issue on the service',
                additionalInfo: err,
                error: !code ? { code: 'general', name: 'General exception' } : code
            }
        }
    }
};

///////////////////////////////////////////////////////////////////////////////////////////////////
// HTTP service: Service API & Webhooks set-up
///////////////////////////////////////////////////////////////////////////////////////////////////
const webServicesServer = express(),
    apiRouter = express.Router(),
    webhookRouter = express.Router();

const loadPlatformWebServices = () => {
    let firstLocalDeploymentWarning = false;
    // process functions
    apiRouter.get('/configuration', (req, res) => {
        logger.info("Configuration request");
        let validToken = false;
        if (settings.token === req?.headers?.token) {
            validToken = true
        }
        if (!validToken && localDeployment) {
            // if the service is running in local environment, pass token validation
            if (!firstLocalDeploymentWarning) {
                firstLocalDeploymentWarning = true;
                logger.info("Invalid or empty token on request. Ignored exceptions of this kind because the service is running in local deployment.");
            }
            validToken = true
        }
        let response = null;
        if (validToken) {
            try {
                response = {
                    app: settings.applicationName,
                    name: settings.svcName,
                    env: settings.environment,
                    perUser: definitions.configurationType === 'PER_USER',
                    configuration: {
                        _svc_name: settings.svcName,
                        _app_name: settings.applicationName,
                        _pod_id: settings.podId,
                        _environment: settings.environment,
                        _local_deployment: settings.localDeployment,
                        _custom_domain: settings.domainCustom,
                        _base_domain: settings.domainBase,
                        _webservices_port: settings.webServicesPort,
                        _debug: settings.debug,
                        _token: '-',
                        _profile: settings.profile,
                        _svcs_services_api: settings.svcsServicesApi,
                        _svc_config: settings.svcConfig
                    },
                    js: '',
                    listeners: '',
                    functions: [],
                    events: []
                };

                if (!!definitions.configurationHelpUrl) {
                    response.configurationHelpUrl = definitions.configurationHelpUrl;
                }
                if (!!definitions.functions) {
                    response.functions = definitions.functions;
                }
                if (!!definitions.events) {
                    response.events = definitions.events;
                }
                if (!!definitions.configuration) {
                    response.conf = definitions.configuration;
                }
                if (!!definitions.userConfiguration) {
                    response.userConf = definitions.userConfiguration;
                }
                if (!!definitions.userConfigurationButtons) {
                    response.userConfButtons = definitions.userConfigurationButtons;
                }
                if (!!definitions.scripts) {
                    let scripts = '';
                    for (let i in definitions.scripts) {
                        try {
                            let fileContent = fs.readFileSync('./scripts/' + definitions.scripts[i], 'utf8');
                            if (fileContent) {
                                scripts += '\n/* */\n';
                                scripts += fileContent;
                                scripts += '\n/* */\n';
                            }
                        } catch (err) {
                            logger.warn('JS file [' + definitions.scripts[i] + '] can not be read: ' + convertException(err));
                        }
                    }
                    response.js = scripts;
                }
                if (!!definitions.listeners) {
                    let listeners = '';
                    for (let i in definitions.listeners) {
                        try {
                            let fileContent = fs.readFileSync('./listeners/' + definitions.listeners[i], 'utf8');
                            if (fileContent) {
                                listeners += '\n/* */\n';
                                listeners += fileContent;
                                listeners += '\n/* */\n';
                            }
                        } catch (err) {
                            logger.warn('Listeners file [' + definitions.listeners[i] + '] can not be read: ' + convertException(err));
                        }
                    }
                    response.listeners = listeners;
                }
            } catch (err) {
                if (err && err.message) {
                    response = convertException(err.message)
                } else {
                    err = util.inspect(err, { showHidden: true, depth: null });
                    if (err && err.startsWith("'") && err.endsWith("'")) {
                        err = err.substring(1, err.length - 1);
                    }
                    response = convertException(err)
                }
            }
            if (!response) {
                response = convertException("Empty service response")
            }
            logger.info("Configuration response from service");
            res.send(response);
        } else {
            logger.info("Invalid token when try to get configuration");
            res.status(401).send('Invalid token')
        }
    });
    //add await on function if async
    apiRouter.post('/function', async (req, res, next) => {

        firstLocalDeploymentWarning = false;
        let validToken = false;
        if (req?.headers?.token === settings.token) {
            validToken = true
        }
        if (!validToken && settings.localDeployment) {
            // if the service is running in local environment, pass token validation
            if (!firstLocalDeploymentWarning) {
                firstLocalDeploymentWarning = true;
                logger.warn("Invalid or empty token on request. Ignored exceptions of this kind because the service is running in local deployment.");
            }
            validToken = true
        }
        let response = null;
        let responseCode = 200;
        if (!validToken) {
            logger.info("Invalid token when try to get configuration");
            res.status(401).send('Invalid token')
        } else {
            let body = req.body || {};
            let functionName = body.function;

            if (!functionName) {
                response = convertException('Empty function name', { code: 'argumentException', name: 'Argument invalid' });
                responseCode = 404;
            } else {
                if (svc.functions[functionName]) {
                    try {
                        if (body.params?.__request_params__) body.params = body.params.__request_params__;
                        response = await svc.functions[functionName](body);
                    } catch (err) {
                        if (!!err && !!err.message) {
                            if (!!err.__svc_exception__) {
                                response = convertException(err)
                            } else {
                                response = convertException(err.message)
                            }
                        } else {
                            err = util.inspect(err, { showHidden: true, depth: null });
                            if (!!err && err.startsWith("'") && err.endsWith("'")) {
                                err = err.substring(1, err.length - 1);
                            }
                            response = convertException(err);
                        }
                        responseCode = 500;
                    }
                    if (!response) {
                        response = convertException("Empty service response");
                        responseCode = 400;
                    }

                    res.status(responseCode).send({
                        date: +new Date(),
                        data: response
                    });

                } else {
                    next(new Error('Cannot find function [' + functionName + '] on service [' + svcName + ']'));
                }

            }
        }
    });

    apiRouter.get('/system/alive', (req, res) => {
        validToken = false;
        if (req?.headers?.token === settings.token) {
            validToken = true;
        }
        if (!validToken && settings.localDeployment) {
            // if the service is running in local environment, pass token validation
            if (!firstLocalDeploymentWarning) {
                firstLocalDeploymentWarning = true;
                logger.warn("Invalid or empty token on request. Ignored exceptions of this kind because the service is running in local deployment.");
            }
            validToken = true;
        }
        if (!validToken) {
            logger.info("Invalid token when try to check health");
            res.status(401).send('Invalid token');
        } else {
            res.send({ started: true });
        }
    });
}

const loadDeveloperWebServices = () => {

    for (const webService in svc.webServices) {
        if (svc.webServices.hasOwnProperty(webService)) {
            let currentWebService = svc.webServices[webService]
            let webServiceMethod = currentWebService.method.toUpperCase();
            let webServicePath = currentWebService.path;
            let webServiceHandler = currentWebService.handler;
            if (webServiceMethod === '') throw new Error('Method for webhook must not be empty. Please review the webhooks provided.')
            switch (webServiceMethod) {
                case 'GET':
                    webhookRouter.get(webServicePath, webServiceHandler);
                    break;
                case 'PUT':
                    webhookRouter.put(webServicePath, webServiceHandler);
                    break;
                case 'PATCH':
                    webhookRouter.patch(webServicePath, webServiceHandler);
                    break;
                case 'POST':
                    webhookRouter.post(webServicePath, webServiceHandler);
                    break;
                case 'DELETE':
                    webhookRouter.delete(webServicePath, webServiceHandler);
                    break;
                case 'HEAD':
                    webhookRouter.head(webServicePath, webServiceHandler);
                    break;
                case 'OPTIONS':
                    webhookRouter.options(webServicePath, webServiceHandler);
                    break;
                case 'CONNECT':
                    webhookRouter.connect(webServicePath, webServiceHandler);
                    break;
                case 'TRACE':
                    webhookRouter.trace(webServicePath, webServiceHandler);
                    break;
                default:
                    break;
            }
        }
    }

};

const loadSvcWebServices = () => {

    loadPlatformWebServices();
    loadDeveloperWebServices();

    webServicesServer.use(compression());
    webServicesServer.use(bodyParser.urlencoded({ extended: true })); // configure app to use bodyParser()
    webServicesServer.use(bodyParser.json()); // this will let us get the data from a POST
    webServicesServer.use('/api', apiRouter); // all of our routes will be prefixed with /api
    webServicesServer.use('/', webhookRouter); // all of our routes will be prefixed with nothing

    if (settings.useSsl) {
        // start https service
        const sslCredentials = {
            key: settings.sslKey,
            cert: settings.sslCert
        };
        https.createServer(sslCredentials, webServicesServer).listen(settings.webServicesPort, function () {
            logger.info('Https service ready on port [' + settings.webServicesPort + ']');
        });
    } else {
        // start http service
        http.createServer(webServicesServer).listen(settings.webServicesPort, function () {
            logger.info('Http service ready on port [' + settings.webServicesPort + ']');
        });
    }

}

const startSvc = async () => {

    ; ({ settings, svcConfig, definitions } = require('./services/configuration/configuration'));

    svc.settings = settings;
    svc.svcConfig = svcConfig;
    svc.definitions = definitions;
    if (svc.hooks.onConfigurationReady) hooks.onConfigurationReady();

    await loadSvcServices();
    if (svc.hooks.onSvcServicesConfigured) hooks.onSvcServicesConfigured();

    loadSvcWebServices();
    if (svc.hooks.onWebServicesReady) hooks.onWebServicesReady();

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // node.js related events
    ///////////////////////////////////////////////////////////////////////////////////////////////////

    process.on('exit', code => logger.info('Service stopped - exit code [' + code + ']'));
    process.on('SIGINT', () => {
        logger.info('Service stopped');
        process.exit(0)
    });
    process.on('beforeExit', code => {
        hooks.onSvcStop(code);
        logger.info('Stopping service - exit code [' + code + ']')
    });
    process.on('warning', warning => logger.warn('Warning [' + util.inspect(warning, { showHidden: true, depth: null }) + ']'));

    appLogger.info('Service [' + settings.svcName + '] is being initialized');

    logger.info(">>> Memory initial usage: " + util.inspect(process.memoryUsage(), { showHidden: true, depth: null }));

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Service Started
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    appLogger.info('Service [' + settings.svcName + '] started');
    svc.hooks.onSvcStart();

    if (settings.isUsingProxy) require('./services/settings/settings').refreshCache();
};
svc.start = startSvc;

module.exports = svc;
