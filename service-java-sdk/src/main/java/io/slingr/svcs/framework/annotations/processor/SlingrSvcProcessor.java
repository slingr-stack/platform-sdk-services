package io.slingr.svcs.framework.annotations.processor;

import io.slingr.svcs.framework.IHttpSvc;
import io.slingr.svcs.framework.IPerUserSvc;
import io.slingr.svcs.framework.annotations.*;
import io.slingr.svcs.framework.annotations.classes.*;
import io.slingr.svcs.services.exchange.ReservedName;
import io.slingr.svcs.services.rest.RestMethod;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Processor that find and parse the annotations declared on the Slingr services
 *
 * <p>Created by lefunes on 28/10/16.
 */
public class SlingrSvcProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Filer filer;

    private ClassSvc slingrClassSvc = null;
    private boolean generatedRunner = false;

    @Override
    public synchronized void init(ProcessingEnvironment environment){
        super.init(environment);
        elementUtils = environment.getElementUtils();
        filer = environment.getFiler();

        AnnotationsUtils.set(environment);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        final Set<String> annotations = new LinkedHashSet<>();
        annotations.add(SlingrService.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Iterate over all @SlingrService annotated elements
        for (Element sSvcElement : roundEnv.getElementsAnnotatedWith(SlingrService.class)) {
            AnnotationsUtils.note(sSvcElement, "Processing [@%s] on [%s] element ", ClassSvc.SE_NAME, sSvcElement.getSimpleName());

            // Check if a class has been annotated with @SlingrService
            if (sSvcElement.getKind() == ElementKind.CLASS) {
                try {
                    // create service class and validate
                    final ClassSvc classSvc = new ClassSvc(sSvcElement);
                    if (!classSvc.isValidClass()) {
                        return true; // Error message printed, exit processing
                    }

                    // check that there is only one service defined
                    if(slingrClassSvc != null){
                        AnnotationsUtils.error(sSvcElement, "Conflict: The class [%s] is annotated with @%s but [%s] already implements the service", classSvc.getQualifiedName(), ClassSvc.SE_NAME, slingrClassSvc.getQualifiedName());
                        return false;
                    }

                    final String simpleName = classSvc.getSimpleName();
                    AnnotationsUtils.note(sSvcElement, "[%s] is a valid service", simpleName);

                    TypeElement currentClass = classSvc.getTypeElement();
                    processElements(roundEnv, currentClass, classSvc);

                    slingrClassSvc = classSvc;

                    // special service implementations to check
                    boolean addedPerUserElements = false;
                    boolean addedHttpElements = false;

                    // Check inheritance: process elements on superclasses until Service
                    while (true) {

                        TypeMirror superClassType = currentClass.getSuperclass();
                        if(superClassType instanceof NoType) {
                            break;
                        }
                        final String canonicalName = superClassType.toString();
                        currentClass = (TypeElement) AnnotationsUtils.asElement(superClassType);

                        // check PER USER services
                        if(!addedPerUserElements && currentClass.getInterfaces().stream().map(Object::toString).anyMatch(s -> IPerUserSvc.class.getCanonicalName().equals(s))){
                            AnnotationsUtils.note(currentClass, "Processing PER USER elements > "+canonicalName);
                            classSvc.registerCodeGenerator(builder -> builder.addCode("\n").addComment("PER USER functions and events are initialized."));

                            currentClass.getEnclosedElements().forEach(element -> {
                                final String name = element.getSimpleName().toString();
                                if(element.getKind() == ElementKind.METHOD) {
                                    switch (name) {
                                        case "defaultMethodConnectUsers": {
                                            registerDefaultFunction(ReservedName.CONNECT_USER, "PER USER connect", classSvc, element, name, IPerUserSvc.class);
                                            break;
                                        }
                                        case "defaultMethodDisconnectUsers": {
                                            registerDefaultFunction(ReservedName.DISCONNECT_USER, "PER USER disconnect", classSvc, element, name, IPerUserSvc.class);
                                            break;
                                        }
                                        case "defaultExternalConnectUser": {
                                            registerDefaultWebService("WEBHOOK", classSvc, element, "/sys/users/{userId}/connect", Arrays.asList(RestMethod.PUT, RestMethod.POST), IPerUserSvc.class);
                                            break;
                                        }
                                        case "defaultExternalDisconnectUser": {
                                            registerDefaultWebService("WEBHOOK", classSvc, element, "/sys/users/{userId}/disconnect", Arrays.asList(RestMethod.PUT, RestMethod.POST), IPerUserSvc.class);
                                            break;
                                        }
                                    }
                                }
                            });

                            addedPerUserElements = true;
                        }

                        // check HTTP svcsAbstractHttpSlingrSvc
                        if(!addedHttpElements && currentClass.getInterfaces().stream().map(Object::toString).anyMatch(s -> IHttpSvc.class.getCanonicalName().equals(s))){
                            AnnotationsUtils.note(currentClass, "Processing HTTP elements > "+canonicalName);
                            classSvc.registerCodeGenerator(builder -> builder.addCode("\n").addComment("HTTP functions and events are initialized."));

                            currentClass.getEnclosedElements().forEach(element -> {
                                final String name = element.getSimpleName().toString();
                                if(element.getKind() == ElementKind.METHOD) {
                                    switch (name) {
                                        case "defaultGetRequest": {
                                            registerDefaultFunction(classSvc.getFunctionPrefix()+"get", "GET", classSvc, element, name, IHttpSvc.class);
                                            break;
                                        }
                                        case "defaultPostRequest": {
                                            registerDefaultFunction(classSvc.getFunctionPrefix()+"post", "POST", classSvc, element, name, IHttpSvc.class);
                                            break;
                                        }
                                        case "defaultPutRequest": {
                                            registerDefaultFunction(classSvc.getFunctionPrefix()+"put", "PUT", classSvc, element, name, IHttpSvc.class);
                                            break;
                                        }
                                        case "defaultDeleteRequest": {
                                            registerDefaultFunction(classSvc.getFunctionPrefix()+"delete", "DELETE", classSvc, element, name, IHttpSvc.class);
                                            break;
                                        }
                                        case "defaultHeadRequest": {
                                            registerDefaultFunction(classSvc.getFunctionPrefix()+"head", "HEAD", classSvc, element, name, IHttpSvc.class);
                                            break;
                                        }
                                        case "defaultPatchRequest": {
                                            registerDefaultFunction(classSvc.getFunctionPrefix()+"patch", "PATCH", classSvc, element, name, IHttpSvc.class);
                                            break;
                                        }
                                        case "defaultOptionsRequest": {
                                            registerDefaultFunction(classSvc.getFunctionPrefix()+"options", "OPTIONS", classSvc, element, name, IHttpSvc.class);
                                            break;
                                        }
                                        case "defaultWebhookProcessor": {
                                            registerDefaultWebService("WEBHOOK", classSvc, element, "/", RestMethod.all(), IHttpSvc.class);
                                            break;
                                        }
                                    }
                                }
                            });

                            addedHttpElements = true;
                        }
                    }

                } catch (IllegalArgumentException e) {
                    // @SlingrService.name() is empty, @SlingrService.label() is empty, ...
                    AnnotationsUtils.error(sSvcElement, e.getMessage());
                    return true;
                }
            } else {
                AnnotationsUtils.error(sSvcElement, "[@%s] is used on [%s]. The annotation only can be used on classes.", sSvcElement.getSimpleName());
                return true;
            }
        }

        // generate the runner that connects the @SlingrService to the framework
        if(!generatedRunner) {
            try {
                if (slingrClassSvc != null) {
                    AnnotationsUtils.note(slingrClassSvc.getTypeElement(), "Generating runner for service [%s]", slingrClassSvc.getTypeElement().getSimpleName());
                    slingrClassSvc.generateRunner(elementUtils, filer);

                    generatedRunner = true;
                    return false;
                } else {
                    AnnotationsUtils.error(null , "Service [@%s] not found", SlingrService.class.getSimpleName());
                }
            } catch (Exception e) {
                AnnotationsUtils.error(slingrClassSvc != null ? slingrClassSvc.getTypeElement() : null, "Exception when generates runner [%s]: %s", e.getClass(), e.getMessage());
            }
        }
        return true; // allow others to process this annotation type
    }

    /**
     * Register a default implementation of a predefined function
     *
     * @param svcFunction name of the function
     * @param functionLabel label of the function
     * @param svcClass class used to register the function
     * @param element element to process
     * @param elementName element name
     * @param methodClass class that implements the method
     */
    private void registerDefaultFunction(String svcFunction, String functionLabel, ClassSvc svcClass, Element element, String elementName, Class<?> methodClass) {
        AnnotationsUtils.note(element, "- default %s function [%s]", functionLabel, elementName);
        if(!svcClass.isFunctionRegistered(svcFunction)) {
            final ClassFunction function = new ClassFunction(element, svcFunction, methodClass, true);
            AnnotationsUtils.note(element, "  - processor [%s]", function.getName());
            svcClass.registerFunction(function);
        } else {
            AnnotationsUtils.note(element, "  - %s function processor is already registered", functionLabel);
        }
    }

    /**
     * Register a default implementation of a predefined web service
     *
     * @param webServiceLabel label of the web service
     * @param svcClass class used to register the function
     * @param element element to process
     * @param processorPath path of the web service
     * @param processorMethods HTTP methods of the web service
     * @param methodClass class that implements the method
     */
    private void registerDefaultWebService(String webServiceLabel, ClassSvc svcClass, Element element, String processorPath, List<RestMethod> processorMethods, Class<?> methodClass) {
        final String path = ClassWebService.normalizePath(processorPath);
        final List<RestMethod> methods;
        if(processorMethods == null || processorMethods.isEmpty()){
            methods = RestMethod.all();
        } else {
            methods = processorMethods;
        }

        for (RestMethod method : methods) {
            registerDefaultWebService(webServiceLabel, svcClass, element, path, method, methodClass);
        }
    }

    /**
     * Register a default implementation of a predefined web service
     *
     * @param webServiceLabel label of the web service
     * @param svcClass class used to register the function
     * @param element element to process
     * @param processorPath path of the web service
     * @param processorMethod HTTP method of the web service
     * @param methodClass class that implements the method
     */
    private void registerDefaultWebService(String webServiceLabel, ClassSvc svcClass, Element element, String processorPath, RestMethod processorMethod, Class<?> methodClass) {
        AnnotationsUtils.note(element, "- default %s web service [%s %s]", webServiceLabel, processorMethod, processorPath);
        if(!svcClass.isWebServiceRegistered(processorPath, processorMethod)) {
            final ClassWebService webService = new ClassWebService(element, processorPath, new RestMethod[]{processorMethod}, methodClass, true);
            AnnotationsUtils.note(element, "  - processor [%s]", webService.getName());
            svcClass.registerWebService(webService);
        } else {
            AnnotationsUtils.note(element, "  - %s web service processor is already registered", webServiceLabel);
        }
    }

    /**
     * Registers each element in the service element on the class service instance
     *
     * @param processor the annotation processing tool framework
     * @param svcElement element to process
     * @param classSvc class used to register the elements
     */
    private void processElements(RoundEnvironment processor, Element svcElement, ClassSvc classSvc) {
        final String simpleName = classSvc.getSimpleName();

        AnnotationsUtils.note(svcElement, "Processing config on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceConfiguration.class).stream()
                .filter(sPropertyElement -> sPropertyElement.getEnclosingElement().equals(svcElement))
                .filter(sPropertyElement -> sPropertyElement.getKind() == ElementKind.FIELD)
                .forEach(sPropertyElement -> {
                    AnnotationsUtils.note(sPropertyElement, "- Service configuration [%s]", sPropertyElement.toString());
                    final ClassProperty property = new ClassProperty(sPropertyElement, true);
                    classSvc.registerProperty(property);
                });
        AnnotationsUtils.note(svcElement, "Config processed on [%s]: [%s]", simpleName, classSvc.getPropertiesNames());

        AnnotationsUtils.note(svcElement, "Processing fields on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceProperty.class).stream()
                .filter(sConfigElement -> sConfigElement.getEnclosingElement().equals(svcElement))
                .filter(sConfigElement -> sConfigElement.getKind() == ElementKind.FIELD)
                .forEach(sConfigElement -> {
                    AnnotationsUtils.note(sConfigElement, "- ClassProperty [%s]", sConfigElement.toString());
                    final ClassProperty property = new ClassProperty(sConfigElement, false);
                    classSvc.registerProperty(property);
                });
        AnnotationsUtils.note(svcElement, "Fields processed on [%s]: [%s]", simpleName, classSvc.getPropertiesNames());

        AnnotationsUtils.note(svcElement, "Processing data stores on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceDataStore.class).stream()
                .filter(sDataStoreElement -> sDataStoreElement.getEnclosingElement().equals(svcElement))
                .filter(sDataStoreElement -> sDataStoreElement.getKind() == ElementKind.FIELD)
                .forEach(sDataStoreElement -> {
                    AnnotationsUtils.note(sDataStoreElement, "- Data store [%s]", sDataStoreElement.toString());
                    final ClassDataStore dataStore = new ClassDataStore(sDataStoreElement);
                    classSvc.registerDataStore(dataStore);
                });
        AnnotationsUtils.note(svcElement, "Data stores processed on [%s]: [%s]", simpleName, classSvc.getDataStoreNames());

        AnnotationsUtils.note(svcElement, "Processing user data stores on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceUserDataStore.class).stream()
                .filter(sUserDataStoreElement -> sUserDataStoreElement.getEnclosingElement().equals(svcElement))
                .filter(sUserDataStoreElement -> sUserDataStoreElement.getKind() == ElementKind.FIELD)
                .forEach(sUserDataStoreElement -> {
                    AnnotationsUtils.note(sUserDataStoreElement, "- User data store [%s]", sUserDataStoreElement.toString());
                    final ClassUserDataStore userDataStore = new ClassUserDataStore(sUserDataStoreElement);
                    classSvc.registerUserDataStore(userDataStore);
                });
        AnnotationsUtils.note(svcElement, "User data stores processed on [%s]: [%s]", simpleName, classSvc.getUserDataStoreNames());

        AnnotationsUtils.note(svcElement, "Processing app loggers on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ApplicationLogger.class).stream()
                .filter(sLoggerElement -> sLoggerElement.getEnclosingElement().equals(svcElement))
                .filter(sLoggerElement -> sLoggerElement.getKind() == ElementKind.FIELD)
                .forEach(sLoggerElement -> {
                    AnnotationsUtils.note(sLoggerElement, "- App logger [%s]", sLoggerElement.toString());
                    final ClassApplicationLogger appLogger = new ClassApplicationLogger(sLoggerElement);
                    classSvc.registerAppLogger(appLogger);
                });
        AnnotationsUtils.note(svcElement, "App loggers processed on [%s]: [%s]", simpleName, classSvc.getAppLoggerNames());

        AnnotationsUtils.note(svcElement, "Processing functions on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceFunction.class).stream()
                .filter(sFunctionElement -> sFunctionElement.getEnclosingElement().equals(svcElement))
                .filter(sFunctionElement -> sFunctionElement.getKind() == ElementKind.METHOD)
                .forEach(sFunctionElement -> {
                    AnnotationsUtils.note(sFunctionElement, "- Function [%s]", sFunctionElement.toString());
                    final ClassFunction function = new ClassFunction(sFunctionElement, false);
                    AnnotationsUtils.note(sFunctionElement, "  - processor [%s]", function.getName());
                    classSvc.registerFunction(function);
                });
        processor.getElementsAnnotatedWith(ServiceFunctions.class).stream()
                .filter(sFunctionElement -> sFunctionElement.getEnclosingElement().equals(svcElement))
                .filter(sFunctionElement -> sFunctionElement.getKind() == ElementKind.METHOD)
                .forEach(sFunctionElement -> {
                    AnnotationsUtils.note(sFunctionElement, "- Function (multiple) [%s]", sFunctionElement.toString());
                    for (ServiceFunction serviceFunction : sFunctionElement.getAnnotationsByType(ServiceFunction.class)) {
                        final ClassFunction function = new ClassFunction(sFunctionElement, serviceFunction, false);
                        AnnotationsUtils.note(sFunctionElement, "  - processor [%s]", function.getName());
                        classSvc.registerFunction(function);
                    }
                });
        AnnotationsUtils.note(svcElement, "Functions processed on [%s]: [%s]", simpleName, classSvc.getFunctionsNames());

        AnnotationsUtils.note(svcElement, "Processing web services on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceWebService.class).stream()
                .filter(sWebServiceElement -> sWebServiceElement.getEnclosingElement().equals(svcElement))
                .filter(sWebServiceElement -> sWebServiceElement.getKind() == ElementKind.METHOD)
                .forEach(sWebServiceElement -> {
                    AnnotationsUtils.note(sWebServiceElement, "- Web service [%s]", sWebServiceElement.toString());
                    final ClassWebService webService = new ClassWebService(sWebServiceElement, false);
                    AnnotationsUtils.note(sWebServiceElement, "  - processor [%s]", webService.getName());
                    classSvc.registerWebService(webService);
                });
        processor.getElementsAnnotatedWith(ServiceWebServices.class).stream()
                .filter(sWebServicesElement -> sWebServicesElement.getEnclosingElement().equals(svcElement))
                .filter(sWebServicesElement -> sWebServicesElement.getKind() == ElementKind.METHOD)
                .forEach(sWebServicesElement -> {
                    AnnotationsUtils.note(sWebServicesElement, "- Web services (multiple) [%s]", sWebServicesElement.toString());
                    for (ServiceWebService serviceWebService : sWebServicesElement.getAnnotationsByType(ServiceWebService.class)) {
                        final ClassWebService webService = new ClassWebService(sWebServicesElement, serviceWebService, false);
                        AnnotationsUtils.note(sWebServicesElement, "  - processor [%s]", webService.getName());
                        classSvc.registerWebService(webService);
                    }
                });
        AnnotationsUtils.note(svcElement, "Web services processed on [%s]: [%s]", simpleName, classSvc.getWebServicesNames());
    }
}
