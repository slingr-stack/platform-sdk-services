const dataStores = require('../dataStores/dataStores');
const events = require('../events/events');

const USER_DATASTORE_NAME = '__svc_users__';
const USER_CONNECTED_EVENT = 'userConnected';
const USER_DISCONNECTED_EVENT = 'userDisconnected';

const findById = async (userId) => {
    if (!userId) {
        throw new Error('User id cannot be empty.')
    }
    try {
        const user = await dataStores.findById(USER_DATASTORE_NAME, userId);
    } catch (error) {

    }
    return user;
};

const save = async (userId, userInfo) => {
    if (!userId) {
        throw new Error('User id cannot be empty.')
    }
    try {
        const user = await dataStores.findById(USER_DATASTORE_NAME, userId);
    } catch (error) {

    }
    try {
        const savedUser = await dataStores.save(USER_DATASTORE_NAME, user, userInfo);
    } catch (error) {

    }
    return savedUser;
};

const removeById = async (userId) => {
    if (!userId) {
        throw new Error('User id cannot be empty.')
    }
    try {
        const user = await dataStores.removeById(USER_DATASTORE_NAME, userId);
    } catch (error) {

    }
};

//Kind of a check and update
const checkUserConnection = async (userId, newInfo) => {
    if (!userId) {
        throw new Error('User id cannot be empty.')
    }
    try {
        const user = await dataStores.findById(USER_DATASTORE_NAME, userId);
    } catch (error) {

    }
    if (!user) {
        throw new Error('User with id [' + userId + '] is not connected.');
    }
    user = {
        ...user,
        ...newInfo
    };
    return user;
}

const connect = async (userId, userInfo, functionId) => {
    if (!userId) {
        throw new Error('User id cannot be empty.')
    }
    let user;
    try {
        user = await save(userId, userInfo);
    } catch (error) {

    }
    sendUserConnectedEvent(userId, userInfo, functionId);
    return user;
}

const disconnect = async (userId, functionId) => {
    if (!userId) {
        throw new Error('User id cannot be empty.')
    }
    let user;
    try {
        await removeById(userId);
    } catch (error) {

    }
    sendUserDisconnectedEvent(userId, null, functionId);
    return user;
}

const sendUserConnectedEvent = (userId, userInfo, functionId) => {
    events.send(USER_CONNECTED_EVENT, userInfo, functionId, userId);
}

const sendUserDisconnectedEvent = (userId, functionId) => {
    events.send(USER_DISCONNECTED_EVENT, null, functionId, userId);
}

module.exports = {
    findById,
    save,
    removeById,
    connect,
    disconnect,
    sendUserConnectedEvent,
    sendUserDisconnectedEvent
}