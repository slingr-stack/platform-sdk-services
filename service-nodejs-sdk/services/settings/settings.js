const httpModule = require('../httpModule/httpModule');
const {setPropertyOnSettings} = require('../configuration/configuration');


const checkProxyStatus = async () => {
    try {
        let res = await httpModule.get('/services/configuration');
        let data = res.data;
        return data.proxy
    } catch (err) {
        console.log("err is:",err);
    }
}

const refreshCache = async () => {
        try {
            await httpModule.put('/services/management/clearCache');
        } catch (err) {
            console.log("err is:",err);
        }
}

module.exports = {
    checkProxyStatus,
    refreshCache 
}