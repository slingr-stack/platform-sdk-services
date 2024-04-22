const 
    logger = require('./logs'),
    {
        applicationName,
        svcName,
        environment
    } = require('../configuration/configuration').settings,
    httpModule = require('../httpModule/httpModule');

 // app log (POST /api/services/logs)
const sendAppLog = (level, message, additionalInfo) => {
    if (!additionalInfo) {
        additionalInfo = {}
    }
    additionalInfo.app = applicationName;
    additionalInfo.svc = svcName;
    additionalInfo.env = environment;

    let appLog = {
        date: +new Date(),
        level: level,
        message: message,
        additionalInfo: additionalInfo
    };
    httpModule.post('/services/logs', appLog)
        .then(body => logger.debug('[APP LOG][' + level + '] ' + appLog.message))
        .catch(error => {
            logger.debug('[APP LOG][' + level + '] ' + appLog.message + ' >> [NO SENT]');
            logger.error('Error when try to send app log to ES [' + error + ']')
        })
};
const appLogDebug = (message, additionalInfo) => sendAppLog('DEBUG', message, additionalInfo);
const appLogInfo = (message, additionalInfo) => sendAppLog('INFO', message, additionalInfo);
const appLogWarn = (message, additionalInfo) => sendAppLog('WARN', message, additionalInfo);
const appLogError = (message, additionalInfo) => sendAppLog('ERROR', message, additionalInfo);

module.exports = {
    debug: appLogDebug,
    info: appLogInfo,
    warn: appLogWarn,
    error: appLogError
}