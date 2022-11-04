---
title: Services Nodejs SDK
keywords: 
last_updated: Nov 1, 2022
tags: []
summary: "Nodejs SDK to create Slingr Services."
sidebar: extensions_sidebar
permalink: extensions_node_sdk.html
folder: extensions
---

This document will guide through the creation of a service using the Nodejs SDK and will provide details about the
framework.

# Create your new service project

## package.json file

The `package.json` contains a few things that you may want to modify:

- `name`: This a human-readable name for your service.
- `version`: This is the version of your service. You can leave `1.0.0` as this version has nothing
  to do with the versions registered in SLINGR, which uses the tags in your repository instead.
- `description`: Description of your what this service is about.
- `keywords`: Here you can set some keywords related to your service.
  
## Service descriptor

The file `service.json` contains at least two fields that you will want to update:

- `label`: this is the human-friendly name of the service.
- `name`: this is the internal name of the service and must match the name you use to register the service
  in SLINGR.

To understand the other settings, you want to take a look at [Services features]({{site.baseurl}}/extensions_common_features.html).

## Reading configuration

You can access the service configuration like this (always inside a function):

```js
svc.functions.someFunction = (svcRequest) => {
    const configs = svc.svcConfig;
    //your code...
} 
``` 

## Hooks

There are a few hooks in services that you can use to perform some initializations or clean up.

```js
svc.hooks.onConfigurationReady = () => {
    //Some code here...
}
svc.hooks.onSvcServicesConfigured = () => {
    //Some code here...
}
svc.hooks.onWebServicesReady = () => {
    //Some code here...
}
svc.hooks.onSvcStart = () => {
    //Some code here...
}
//This one receives a 'cause' parameter wich is the 'code' of the process.on('beforeExit') event
svc.hooks.onSvcStop = (cause) => {
    //Some code here...
}
```

## Functions

To implement a function that is defined in the `service.json` file, you should do the following:

```js
svc.functions.yourFunctionName = (svcRequest) => {
    //You can access all the service services here like svc.svcConfig or svc.dataStores
    //Your custom code goes here...
    return { someInfo: 'someValue'}
}
``` 
Service functions will get a request parameter that includes the parameters sent to the function (among other info). 
You must always return a `Json` object with the response. 

## Events

You can send events to the app using the events' property of the service.
You can send an async event, or a sync one if you expect a response from the app:

```js
svc.functions.fnThatSendsAsyncEvent = (svcRequest) => {
    const requestId = svcRequest.id;
    //Later in your code...
    svcs.events.send('someEventName', data, requestId);
}

svc.functions.fnThatSendsSyncEvent = (svcRequest) => {
    const requestId = svcRequest.id;
    //Later in your code...
    let eventResponse = svcs.events.sendSync('someEventName', data, requestId);
    //Do something with that response...
}
```

Keep in mind that the `'someEventName'` should be defined in your `service.json` file, under the `events` property. 
The `data` argument will be the data you want to receive on the event.
Finally, `requestId` will be the request id which can be retrieved from the request parameter on the defined function like shown above.

## Data stores

If you service needs to persist information, data stores are available for services. They need to be defined in the 
`service.json` file and then you can use them in the service.

The available methods to access the various datastores are the following:

```js
svcs.functions.someFunction = async () => {
    //Find documents by some flter: Filter here is the same as the one in sys.data.find(). eg: {someField: 'someValue'}
    svcs.dataStores.someDataStore.find(filter); 
    svcs.dataStores.someDataStore.findOne(filter);
    //Find document by id:
    svcs.dataStores.someDataStore.findById('documentId');
    //Save a document: Some object will be any javascript object.
    svcs.dataStores.someDataStore.save(someObject);
    //Update a document:
    svcs.dataStores.someDataStore.update('documentId',someObject);
    //Remove documents by filter:
    svcs.dataStores.someDataStore.remove(filter);
    //Remove by id:
    svcs.dataStores.someDataStore.removeById('documentId',someObject);
    //Count the documents currently saved in the store by filter:
    svcs.dataStores.someDataStore.count(filter);
}
```
You can either `await` the response or use the `then()` block depending on your needs.

## Webservices

If you want your service to receive calls over HTTP, you can define them with the `webServices` property. You must define it
as an object which will contain the `method`, `path` and `handler` of the webservice:

```js
svc.webServices.nameForYourWebService = {
    method: 'POST',
    path: '/',
    //As this is an express service, you receive the req, and res objects
    handler: (req, res) => {
        //Do something... and then return a response to the caller 
        res.json({status: 'ok'})
    }
}
```

Given the above example, the following URL will be available and listening to requests:

```
POST https://<yourAppName>.slingrs.io/<env>/svcs/<svcName>
```

When that URL is called, the handler will be invoked.

{% include callout.html content="You should always add some kind of verification (like a token) to avoid anyone calling your services." type="warning" %} 

## Handling files

It is possible to upload and download files to/from the app using the utilities in the property `files`.
If you want to handle the processing in a sync or async way, you will need to either `await` the service, or handle the 
response in the `then()` block and send an event to the platform.

Both scenarios are shown below:

```js
svc.functions.asyncDownloadFileFromSvc = async (svcRequest) => {
    const file = svcRequest.params;
    svc.files.download(file.id).then(
        (res) => {
            svc.logger.info('File download has completed!');
            //In this case we return res.toString() because we know the file being downloaded is a .txt. Its not recommended to return the plain buffer to the platform.
            svc.events.send('onDownloadComplete', res.toString(), svcRequest.id)
        }
    );
    return { msg: 'File [' + file.id + '] is being downloaded and the processing will be made asynchronously. An event will be fired when the download is complete.' }
};

svc.functions.syncDownloadFileFromSvc = async (svcRequest) => {
    const file = svcRequest.params;
    var fileResponse = await svc.files.download(file.id);
    svc.logger.info('File download has completed!');
    //In this case we return res.toString() because we know the file being downloaded is a .txt. Its not recommended to return the plain buffer to the platform.
    return { fileContents: fileResponse.toString() }
};

svc.functions.uploadFileSyncFromSvc = async (svcRequest) => {
    const fileUrl = 'https://jsoncompare.org/LearningContainer/SampleFiles/PDF/sample-pdf-with-images.pdf';
    try {
        //We download the dummy file from an HTTP request
        var downloadResponse = await svc.httpModule.get(fileUrl);
    } catch (error) {
        svc.logger.error('Couldn\'t download the file from [' + fileUrl + '].', error);
    }
    //And upload it to the platform
    var fileInfo = await svc.files.upload('somefile.pdf', downloadResponse.data);
    //The info is returned to the app synchronously 
    return fileInfo;
};

svc.functions.uploadFileAsyncFromSvc = (svcRequest) => {

    const fileUrl = 'https://jsoncompare.org/LearningContainer/SampleFiles/PDF/sample-pdf-with-images.pdf';
    //We download the dummy file from an HTTP request
    svc.httpModule.get(fileUrl).then(
        (downloadResponse) => {
            //And upload it to the platform
            svc.files.upload('somefile.pdf', downloadResponse.data).then(
                (fileInfo) => {
                    //In this case, the info will be sent asynchronously via events
                    svc.events.send('onUploadComplete', fileInfo, svcRequest.id);
                }
            ).catch(
                (err) => {
                    svc.logger.error('Couldn\'t upload the file to platform.', err);
                }
            );
        }
    ).catch(
        (err) => {
            svc.logger.error('Couldn\'t download the file from [' + fileUrl + '].', err);
        }
    );

    return { msg: 'A file will be downloaded and then uploaded to the platform. This processing will be made asynchronously. An event will be fired when the download/upload is complete.' }
};
```

{% include important.html content="Remember that the events must be defined in the **`service.json`** file, and if you are using callbacks, also in the function's callbacks array property."%}

## Logging

It is possible to send logs to the app from your service using the `AppLogs`:

```js
svc.functions.someFunctionThatLogs = (svcRequest) => {
    svc.appLogger.debug('Function executed!')
    svc.appLogger.info('Function executed!')
    svc.appLogger.warn('Function executed!')
    svc.appLogger.error('Function executed!')
}
```

You can send additional information that will be displayed when you click on `More Info` in the log in the app monitor by sending
a second parameter to the appLogger functions like this:

```js
svc.functions.someFunctionThatLogs = (svcRequest) => {
    svc.appLogger.debug('Function executed!',someObjectOrMessage)
    svc.appLogger.info('Function executed!',someObjectOrMessage)
    svc.appLogger.warn('Function executed!',someObjectOrMessage)
    svc.appLogger.error('Function executed!',someObjectOrMessage)
}
```

*Debug logs will only be shown in dev and staging environments monitor.*

# Creating a proxy service

Before you can run your service locally, you should set up a proxy service in the app you will be using to test
the development of your service. You can find more information about this in [Create your own services]({{site.baseurl}}/extensions_create_your_own_services.html).

When you add a new `Proxy service` to you app, you will be asked to enter the `Service URI` in the configuration. We
recommend to use [ngrok](https://ngrok.com/) instead of opening a port in your router. With `ngrok` you can set up
a URI like this:

```
./ngrok http 10000
```

This will give you an HTTP and HTTPS URL. We recommend using the HTTPS URL, so copy it into the configuration of
your service.

Regarding the token we recommend to leave the autogenerated token, except that you have a reason not to do that.

Once you create the service, you will see the configuration below, something like this:

```
_svc_name=proxy
_app_name=yourtestapp
_environment=dev
_pod_id=id
_profile=default
_custom_domain=
_debug=true
_local_deployment=true
_base_domain=slingrs.io
_webservices_port=10000
_svcs_services_api=https://yourtestapp.slingrs.io/dev/svcs/proxy/api
_token=91833a8b-929f-4eab-b7b4-2383c10cd629
_svc_config={}
```

You should copy this configuration to `.env` file. Keep in mind that the last property, `_svc_config`,
should have a valid JSON with the config of your service, so you might want to override that. 
If you used the skeleton service you should have something like this:

```
_svc_name=proxy
_app_name=yourtestapp
_environment=dev
_pod_id=id
_profile=default
_custom_domain=
_debug=true
_local_deployment=true
_base_domain=slingrs.io
_webservices_port=10000
_svcs_services_api=https://yourtestapp.slingrs.io/dev/svcs/proxy/api
_token=91833a8b-929f-4eab-b7b4-2383c10cd629
_svc_config={"token":"123456"}
``` 

Keep in mind that `.env` is only used when you run the service locally, but it does not affect the service when running on the cloud because service config is passed in another way.

*If you need, you can have multiple `.env` for different environments or setups. For example, you could have additional `.staging.env` or `.myCustomEnv.env` files. In this case, you should execute your service setting the `NODE_ENV` environment variable to match the name of the file. [Here it's how set env variables in different OSs and terminals](https://stackoverflow.com/a/9204973).
By default, the `.env` file is loaded.*

Once you have created the proxy service in your app, remember to push changes to initialize it.

# Running your service

Before running your service, make sure that install all the dependencies:

```
cd SERVICE_FOLDER
npm install
```

Then you can run your service from the command line or using your IDE:

```
node service.js
```
or
```
npm start
```
Or you can customize your own start script from the `package.json` file.

# Testing that your service is working

Now that the service is running and the proxy service is set up, we can do a quick test to verify everything
is working. In order to do that execute the following code in your builder or monitor console:

```js
var res = app.svcs.proxy.randomNumber({});
log('res: '+JSON.stringify(res));
``` 

You should see an output like this:

```
res: {"number":5560} 
```

We are assuming that you are using the skeleton service template where this method is available. Otherwise,
you should call a method that exists in your service.

# More samples

There are dozens of services already developed for the SLINGR platform. You can take a look at them to see
more features in the services' framework:

[https://github.com/slingr-stack](https://github.com/slingr-stack)

{% include links.html %}
