const definitions = require('../configuration/configuration').definitions;
const httpModule = require('../httpModule/httpModule');

const DATASTORES_API_PATH = '/services/dataStores/';

const isEmpty = (obj) => {
    return obj 
        && Object.keys(obj).length === 0
        && Object.getPrototypeOf(obj) === Object.prototype;
}

const dataStores = {
    find: async (dataStoreName, filter) => {
        if (isEmpty(filter)) filter = null;
        const dataStoreResponse = await httpModule.get(DATASTORES_API_PATH+dataStoreName,{
            params: {
                ...filter
            }
        });
        return dataStoreResponse.data;
    },
    findById: async (dataStoreName,documentId) => {
        if (!documentId || documentId === '') throw new Error('The documentId can\'t be empty');
        const dataStoreResponse = await httpModule.get(DATASTORES_API_PATH+dataStoreName+'/'+documentId);
        return dataStoreResponse.data;
    },
    save: async (dataStoreName,document) => {
        if (isEmpty(document)) throw new Error('The document to save can\'t be empty');
        const dataStoreResponse = await httpModule.post(DATASTORES_API_PATH+dataStoreName,document);
        return dataStoreResponse.data;
    },
    update: async (dataStoreName,documentId,newDocument) => {
        if (!documentId) throw new Error('The documentId can\'t be empty');
        if (isEmpty(newDocument)) throw new Error('The document to update can\'t be empty');
        const dataStoreResponse = await httpModule.put(DATASTORES_API_PATH+dataStoreName+'/'+documentId,newDocument);
        return dataStoreResponse.data;
    },
    remove: async (dataStoreName,filter) => {
        if (isEmpty(filter)) filter = null;
        const dataStoreResponse = await httpModule.delete(DATASTORES_API_PATH+dataStoreName,{
            params: {
                ...filter
            }
        });
        return dataStoreResponse.data;
    },
    removeById: async (dataStoreName,documentId) => {
        if (!documentId || documentId === '') throw new Error('The documentId can\'t be empty');
        const dataStoreResponse = await httpModule.delete(DATASTORES_API_PATH+dataStoreName+'/'+documentId);
        return dataStoreResponse.data;
    },
    count: async (dataStoreName, filter) => {
        if (isEmpty(filter)) filter = null;
        const dataStoreResponse = await httpModule.get(DATASTORES_API_PATH+dataStoreName+'/count',{
            params: {
                ...filter
            }
        });
        return dataStoreResponse.data;
    }
};

for (const dataStore of definitions.stores) {
    const dataStoreName = dataStore.name;
    dataStores[dataStoreName] = {
        find: async (filter) => {
            return await dataStores.find(dataStoreName,filter);
        },
        findOne: async (filter) => {
            return (await dataStores.find(dataStoreName,filter)).items[0];
        },
        findById: async (documentId) => {
            return await dataStores.findById(dataStoreName,documentId);
        },
        save: async (document) => {
            return await dataStores.save(dataStoreName,document);
        },
        update: async (documentId,newDocument) => {
            return await dataStores.update(dataStoreName,documentId,newDocument);
        },
        remove: async (filter) => {
            return await dataStores.remove(filter);
        },
        removeById: async (documentId) => {
            return await dataStores.removeById(dataStoreName,documentId);
        },
        count: async (filter) => {
            return await dataStores.count(dataStoreName,filter);
        }
    }
}

module.exports = dataStores