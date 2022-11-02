const logger = require('../loggers/logs');
const httpModule = require('../httpModule/httpModule');

const getUserInformationByToken = async (token) => {
    if(!token) {
        throw new Error('User token can\'t be empty.');
    }
    let userInfo;
    try {
        userInfo = await httpModule.get('/users',{
            params: {
                userToken: token
            }
        });
    } catch (error) {
        logger.error('Error while triying to get information by token for token ['+token+'].')
        throw new Error('Error while triying to get information by token for token ['+token+'].')
    }
    if (!userInfo) {
        logger.error('No user found with token ['+token+']');
        throw new Error('No user found with token ['+token+']');
    }
}

const getUserInformationByEmail = async (email) => {
    if(!email) {
        throw new Error('User email can\'t be empty.');
    }
    let userInfo;
    try {
        userInfo = await httpModule.get('/users',{
            params: {
                userEmail: email
            }
        });
    } catch (error) {
        logger.error('Error while triying to get information by email for email ['+email+'].')
        throw new Error('Error while triying to get information by email for email ['+email+'].')
    }
    if (!userInfo) {
        logger.error('No user found with email ['+email+']');
        throw new Error('No user found with email ['+email+']');
    }
}

module.exports = {
    getUserInformationByToken,
    getUserInformationByEmail,
}