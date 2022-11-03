const httpModule = require('../httpModule/httpModule');
const logger = require('../loggers/logs');

const SCRIPTS_API_PATH = '/svcs/scripts'

const execute = async (scriptName, parameters) => {
    if (!scriptName){
        logger.error('Cannot execute script if no name is provided.')
        return;
    }
    let response;
    try {
        response = await httpModule.post(SCRIPTS_API_PATH, {
            date: new Date().getTime(),
            scriptName: scriptName,
            scriptParameters: parameters
        });
    } catch (error) {
        logger.error('Error while trying to execute script ['+scriptName+']',error);
        return;
    }

    return response.data;
}

module.exports = {
    execute
}