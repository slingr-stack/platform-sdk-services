const logger = require('../loggers/logs');
const httpModule = require('../httpModule/httpModule');

const LOCKS_API_PATH = '/locks/';

const acquireLock = async (key) => {
    if(!key) {
        throw new Error('Key can\'t be empty to aquire a lock.');
    }
    let lock;
    try {
        lock = await httpModule.post(LOCKS_API_PATH+key);
    } catch (error) {
        logger.error('Error while triying to get lock for key ['+key+'].')
        throw new Error('Error while triying to get lock for key ['+key+'].')
    }
    return lock;
}

const releaseLock = async (key) => {
    if(!key) {
        throw new Error('Key can\'t be empty to release a lock.');
    }
    let unlock;
    try {
        unlock = await httpModule.delete(LOCKS_API_PATH+key);
    } catch (error) {
        logger.error('Error while triying to release lock for key ['+key+'].')
        throw new Error('Error while triying to release lock for key ['+key+'].')
    }
    return unlock;
}

module.exports = {
    acquireLock,
    releaseLock
}