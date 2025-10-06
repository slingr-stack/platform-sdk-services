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
        // Remove size limit to support large file uploads (e.g., ZIP files with 200+ samples)
        maxContentLength: Infinity,
        maxBodyLength: Infinity
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