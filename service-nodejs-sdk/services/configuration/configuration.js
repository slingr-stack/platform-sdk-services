const fs = require('fs');
//////////////////////////////////////
// Load Configuration from env vars //
//////////////////////////////////////
const {
    // Service constants
    _svc_name: svcName,
    _app_name: applicationName,
    _pod_id: _podId,
    _environment: environment,
    _local_deployment: _localDeployment,
    _debug: _debug,
    // HTTP services properties
    _webservices_port: webServicesPort,
    _token: token,
    _profile: profile,
    _extension_broker_api: _extension_broker_api,
    _useSsl: _useSsl,
    SSL_KEY: sslKey,
    SSL_CERT: sslCert,
    // System properties
    _custom_domain: domainCustom,
    _base_domain: domainBase,
    // Service specific properties
    _svc_config: _svc_config
} = { ...process.env };


const podId = _podId.length > 5 ? _podId.substring(_podId.length - 5) : _podId;
const localDeployment = _localDeployment !== 'false' && !!_localDeployment;
const debug = _debug !== 'false' && !!_debug;
const useSsl = !localDeployment || _useSsl;
const svcConfig = JSON.parse(_svc_config);

const maskToken = token => {
    if (!token) {
        return '-';
    }
    return token.length < 10 ? '.'.repeat(token.length) :
        token.length < 20 ? token.substr(0, 2) + '.'.repeat(token.length - 4) + token.substr(token.length - 2) :
            token.substr(0, 4) + '.'.repeat(token.length - 8) + token.substr(token.length - 4)
};

const maskedToken = maskToken(token);

let cDomain = domainCustom;
if (cDomain) {
    cDomain = (localDeployment ? 'http' : 'https') + '://' + cDomain
} else {
    cDomain = (localDeployment ? 'http' : 'https') + '://' + applicationName + '.' + domainBase + '/' + environment
}
const domain = cDomain.toLowerCase();
const secondaryDomain = ((localDeployment ? 'http' : 'https') + '://' + domainBase + '/' + applicationName + '/' + environment).toLowerCase();

const webhookUrl = domain + '/services/' + svcName;

const proto = useSsl ? 'https' : 'http';
console.log('Configured service [' + svcName + ']: ' +
    proto + ' [0.0.0.0:' + webServicesPort + '], ' +
    'webhook [' + webhookUrl + '], ' +
    'token [' + maskedToken + '], ' +
    (localDeployment ? ', local deployment' : '')
);

const settings = {
    svcName,
    applicationName,
    podId,
    environment,
    localDeployment,
    webServicesPort,
    token,
    maskedToken,
    profile,
    _extension_broker_api,
    webhookUrl,
    useSsl,
    sslKey,
    sslCert,
    domainCustom,
    secondaryDomain,
    domainBase,
    debug,
    svcConfig
}

module.exports = {
    settings,
    svcConfig: settings.svcConfig
};

//Some definitions constants
const USER_CONNECTED_EVENT = 'userConnected',
    USER_DISCONNECTED_EVENT = 'userDisconnected',
    USER_CONNECT_FUNCTION = 'connectUser',
    USER_DISCONNECT_FUNCTION = 'disconnectUser';
    

const logger = require('../loggers/logs');
let definitions;
try {
    definitions = JSON.parse(fs.readFileSync('./appService.json', 'utf8'));
} catch (error) {
    logger.error('Service definitions could not be loaded from \'appService.json\' file: ',error);
}

//If the service is a PER-USER service, we have to load the user datastore, and the user related events 
if (definitions.configurationType === 'PER_USER') {

    // add the USERS data store if this is not defined
    definitions.stores.push({name: '__svc_users__'});
    
    // add PER USER events if these are not defined
    const hasUserConnectedEvent = definitions.events.some((event)=>{event.name===USER_CONNECTED_EVENT});
    if (!hasUserConnectedEvent) {
        definitions.events.push({
            label: 'User connected',
            name: USER_CONNECTED_EVENT,
            eventType: 'PER_USER',
            description: 'Event triggered when the current user is connected to the service.'
        })
    }
    const hasUserDisconnectedEvent = definitions.events.some((event)=>{event.name===USER_DISCONNECTED_EVENT});
    if (!hasUserDisconnectedEvent) {
        definitions.events.push({
            label: 'User disconnected',
            name: USER_DISCONNECTED_EVENT,
            eventType: 'PER_USER',
            description: 'Event triggered when the current user is disconnected from the service.'
        })
    }

    // add PER USER functions if these are not defined
    const hasUserConnectedFunction = definitions.functions.some((func)=>{func.name===USER_CONNECT_FUNCTION});
    if (!hasUserConnectedFunction) {
        definitions.functions.push({
            label: 'Connect User',
            name: USER_CONNECT_FUNCTION,
            eventType: 'PER_USER',
            description: 'Connects the user to the service.',
            callbacks: [
                {
                    name: USER_CONNECTED_EVENT,
                    maxWaitingTime: 60000,
                    maxExpectedResponses: 1
                }
            ]
        })
    }
    const hasUserDisconnectFunction = definitions.functions.some((func)=>{func.name===USER_DISCONNECT_FUNCTION});
    if (!hasUserDisconnectFunction) {
        definitions.functions.push({
            label: 'Disconnect User',
            name: USER_DISCONNECT_FUNCTION,
            eventType: 'PER_USER',
            description: 'Disconnects the user from the service.',
            callbacks: [
                {
                    name: USER_DISCONNECTED_EVENT,
                    maxWaitingTime: 60000,
                    maxExpectedResponses: 1
                },
            ]
        })
    }
    
}


module.exports.definitions = definitions;

