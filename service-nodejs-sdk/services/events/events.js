const util = require('util');
const httpModule = require('../httpModule/httpModule');
const logger = require('../loggers/logs');

const EVENTS_API_PATH = '/services/events';
const EVENTS_SYNC_API_PATH = '/services/events/sync';


let lastStatistic = null;

const handleSendEvent = async (eventName, data, fromFunction, userId, userEmail, isSync) => {
    if (!eventName) {
        throw 'Event name is empty'
    }
    if (!data) {
        data = {}
    }
    let eventBody = {
        date: new Date().getTime(),
        event: eventName,
        data: data,
        ...(fromFunction && { fromFunction: fromFunction }),
        ...(userId && { userId: userId }),
        ...(userEmail && { userEmail: userEmail }),
    };

    let response = null;

    if (isSync) {
        response = await httpModule.post(EVENTS_SYNC_API_PATH, eventBody);
    } else {
        httpModule.post(EVENTS_SYNC_API_PATH, eventBody);
    }
    if (!lastStatistic || (new Date() - 3600000) > lastStatistic) {
        lastStatistic = new Date();
        logger.info(">>> mem usage: " + util.inspect(process.memoryUsage(), { showHidden: true, depth: null }));
    }
    return response;

}
//handle sync responses from platform
const send = (eventName, data, fromFunction, userId, userEmail) => {
    handleSendEvent(eventName, data, fromFunction, userId, userEmail, false).then(
        (data) => {
            logger.debug('[EVENT][' + eventName + '] >> [SENT]')
        }
    ).catch(
        (err) => {
            logger.debug('[EVENT][' + eventName + '] >> [NO SENT]');
            logger.info('Error when try to send event to ES [' + err + ']');
        }
    );
};

const sendSync = async (eventName, data, fromFunction, userId, userEmail) => {
    try {
        let response = await handleSendEvent(eventName, data, fromFunction, userId, userEmail, true);
        logger.debug('[SYNC EVENT][' + eventName + '] >> [SENT]');
        return response.data;
    } catch (error) {
        logger.debug('[SYNC EVENT][' + eventName + '] >> [NO SENT]');
        logger.info('Error when try to send sync event to ES [' + error + ']');
    }
}

module.exports = {
    send,
    sendSync
}