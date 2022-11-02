const {
        podId,
        applicationName,
        svcName,
        environment,
        debug
    } = require('../configuration/configuration').settings;
const dayjs = require('dayjs');

const formatLog = (level, message) => {
    return dayjs().format('YYYY-MM-DD HH:mm:ss.SSS ZZ') + ' ' +
    'comp=svc ' +
    'level=' + level + ' ' +
    'podId=' + podId + ' ' +
    'app=' + applicationName + ' ' +
    'svc=' + svcName + ' ' +
    'env=' + environment + ' ' +
    message;
}

const logDebug = (message, obj) => {
    if (message && debug) {
        if (obj) {
            console.log(formatLog('DEBUG', message),obj);
        } else {
            console.log(formatLog('DEBUG', message));
        }
    }
};

const logInfo = (message, obj) => {
    if (message) {
        if (obj) {
            console.info(formatLog('INFO', message),obj);
        } else {
            console.info(formatLog('INFO', message));
        }
    }
};

const logWarn = (message, obj) => {
    if (message) {
        if (obj) {
            console.warn(formatLog('WARN', message),obj);
        } else {
            console.warn(formatLog('WARN', message));
        }
    }
};

const logError = (message, obj) => {
    if (message) {
        if (message) {
            if (obj) {
                console.error(formatLog('ERROR', message),obj);
            } else {
                console.error(formatLog('ERROR', message));
            }
        }
    }
};

module.exports = {
    debug: logDebug,
    info: logInfo,
    warn: logWarn,
    error: logError
}