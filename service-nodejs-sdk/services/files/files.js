const httpModule = require('../httpModule/httpModule');
const logger = require('../loggers/logs');
const appLogger = require('../loggers/appLogs');
const fs = require('fs');
const stream = require('node:stream');
const pStream = stream.promises;
const FormData = require('form-data');

const FILES_API_PATH = '/svcss/files'
const TMP_FILES_PATH = './tmp/'

const clearTmpFile = async (filePath) => {
    fs.unlink(filePath,(err) => {
        if (err) throw err;
        logger.debug(filePath + ' was deleted.');
      });
}

const createTmpFile = async (fileName, fileContents) => {
    const filePath = TMP_FILES_PATH + new Date().getTime() + '_' + (fileName ? fileName : 'file');
    if (!fs.existsSync(TMP_FILES_PATH)) fs.mkdirSync(TMP_FILES_PATH);
    
    if (fileContents instanceof stream.Stream){
        const writer = fs.createWriteStream(filePath);
        fileContents.pipe(writer);
        await pStream.finished(writer);
    } else {
        fs.writeFileSync(filePath, fileContents);
    }
    return filePath;
}

const download = async (fileId) => {
    if (!fileId) {
        throw new Error('The file ID cannot be empty when downloading files.');
    }
    
    let downloadResponse;
    try {
        downloadResponse = await httpModule.get(FILES_API_PATH + '/' + fileId, {responseType: 'stream'});
    } catch (error) {
        logger.error('Error while downloading file from platform.',error);
    }
    const tmpFilePath = await createTmpFile(fileId, downloadResponse.data);
    const downloadData = fs.readFileSync(tmpFilePath);
    clearTmpFile(tmpFilePath);
    return downloadData;
};

const upload = async (fileName, fileContents) => {
    if (!fileName) {
        logger.error('Cannot upload a file if fileName is empty.');
        appLogger.error('Error while uploading a file because no fileName was provided.');
        return;
    }
    if (!fileContents) {
        logger.error('Cannot upload a file if fileContents is empty.');
        appLogger.error('Error while uploading the file ['+fileName+'] because of empty content.');
        return;
    }

    let tmpFilePath;
    try {
        tmpFilePath = await createTmpFile(fileName, fileContents);
    } catch (err) {
        logger.error('The temporal file for ['+fileName+']could not be created. The file wont be uploaded to the app.',err);
        appLogger.error('Error while uploading the file ['+fileName+']. It wasn\'t uploaded to the app.');
        return;
    }

    const formData = new FormData();
    formData.append('file', fs.createReadStream(tmpFilePath));
    let options = {
        headers: {
            ...formData.getHeaders()
        }
    };

    let fileResponse;
    try {
        fileResponse = await httpModule.post(FILES_API_PATH, formData, options);
        logger.debug('Uploaded file to application');
    } catch (error) {
        logger.info('Error while trying to upload a file to app [' + error + ']');
        appLogger.error('Error while trying to upload a file from service to application');
        return;
    }
    clearTmpFile(tmpFilePath);
    return fileResponse.data;
};

const getFileMetadata = async (fileId) => {
    if (!fileId) {
        logger.error('The fileId cannot be empty when getting files metadata.')
        appLogger.error('Error while getting app metadata because no fileId was provided.')
        return;
    }

    let metadataResponse;
    try {
        metadataResponse = await httpModule.get(FILES_API_PATH+'/'+fileId+'/metadata');
    } catch (error) {
        logger.info('Error when try to fetch metadata from file [' + fileId + ']');
        appLogger.error('Error when try to fetch metadata from file [' + fileId + ']',error);
    }
    logger.debug('File Metadata fetched.');
    return metadataResponse.data;
}

module.exports = {
    download,
    upload,
    getFileMetadata
}