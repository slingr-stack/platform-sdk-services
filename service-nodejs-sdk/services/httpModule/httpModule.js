const https = require('https');

const {
    applicationName,
    environment,
    token,
    _extension_broker_api,
} = require('../configuration/configuration').settings;
const axiosInstance = require('axios').create(
    {
        baseURL: _extension_broker_api,
        headers: {
            app: applicationName,
            env: environment,
            token: token,
            version: 'v1'
        },
        httpsAgent: new https.Agent({
            rejectUnauthorized: false
        }),
        // Set max content length to 100MB to support large file uploads
        maxContentLength: 100 * 1024 * 1024,
        maxBodyLength: 100 * 1024 * 1024
    }
);
// require('axios-retry')(
//     httpModule, {
//         retries: 5,
//         retryDelay: 5000,
//         shouldResetTimeout: true 
//     }
// );

module.exports = axiosInstance;