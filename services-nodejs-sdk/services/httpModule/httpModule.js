const https = require('https');

const {
    applicationName,
    environment,
    token,
    svcsServicesApi,
} = require('../configuration/configuration').settings;
const axiosInstance = require('axios').create(
    {
        baseURL: svcsServicesApi,
        headers: {
            app: applicationName,
            env: environment,
            token: token,
            version: 'v1'
        },
        httpsAgent: new https.Agent({
            rejectUnauthorized: false
        }),
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