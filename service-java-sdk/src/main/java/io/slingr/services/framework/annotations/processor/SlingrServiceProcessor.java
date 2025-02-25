package io.slingr.services.framework.annotations.processor;

import io.slingr.services.framework.IHttpService;
import io.slingr.services.framework.IPerUserService;
import io.slingr.services.framework.annotations.*;
import io.slingr.services.framework.annotations.classes.*;
import io.slingr.services.services.exchange.ReservedName;
import io.slingr.services.services.rest.RestMethod;

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
public class SlingrServiceProcessor extends AbstractProcessor {

    private Elements elementUtils;
    private Filer filer;

    private ClassService slingrClassService = null;
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
        for (Element sServiceElement : roundEnv.getElementsAnnotatedWith(SlingrService.class)) {
            AnnotationsUtils.note(sServiceElement, "Processing [@%s] on [%s] element ", ClassService.SE_NAME, sServiceElement.getSimpleName());

            // Check if a class has been annotated with @SlingrService
            if (sServiceElement.getKind() == ElementKind.CLASS) {
                try {
                    // create service class and validate
                    final ClassService classService = new ClassService(sServiceElement);
                    if (!classService.isValidClass()) {
                        return true; // Error message printed, exit processing
                    }

                    // check that there is only one service defined
                    if(slingrClassService != null){
                        AnnotationsUtils.error(sServiceElement, "Conflict: The class [%s] is annotated with @%s but [%s] already implements the service", classService.getQualifiedName(), ClassService.SE_NAME, slingrClassService.getQualifiedName());
                        return false;
                    }

                    final String simpleName = classService.getSimpleName();
                    AnnotationsUtils.note(sServiceElement, "[%s] is a valid service", simpleName);

                    TypeElement currentClass = classService.getTypeElement();
                    processElements(roundEnv, currentClass, classService);

                    slingrClassService = classService;

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
                        if(!addedPerUserElements && currentClass.getInterfaces().stream().map(Object::toString).anyMatch(s -> IPerUserService.class.getCanonicalName().equals(s))){
                            AnnotationsUtils.note(currentClass, "Processing PER USER elements > "+canonicalName);
                            classService.registerCodeGenerator(builder -> builder.addCode("\n").addComment("PER USER functions and events are initialized."));

                            currentClass.getEnclosedElements().forEach(element -> {
                                final String name = element.getSimpleName().toString();
                                if(element.getKind() == ElementKind.METHOD) {
                                    switch (name) {
                                        case "defaultMethodConnectUsers": {
                                            registerDefaultFunction(ReservedName.CONNECT_USER, "PER USER connect", classService, element, name, IPerUserService.class);
                                            break;
                                        }
                                        case "defaultMethodDisconnectUsers": {
                                            registerDefaultFunction(ReservedName.DISCONNECT_USER, "PER USER disconnect", classService, element, name, IPerUserService.class);
                                            break;
                                        }
                                        case "defaultExternalConnectUser": {
                                            registerDefaultWebService("WEBHOOK", classService, element, "/sys/users/{userId}/connect", Arrays.asList(RestMethod.PUT, RestMethod.POST), IPerUserService.class);
                                            break;
                                        }
                                        case "defaultExternalDisconnectUser": {
                                            registerDefaultWebService("WEBHOOK", classService, element, "/sys/users/{userId}/disconnect", Arrays.asList(RestMethod.PUT, RestMethod.POST), IPerUserService.class);
                                            break;
                                        }
                                    }
                                }
                            });

                            addedPerUserElements = true;
                        }

                        // check HTTP servicesAbstractHttpSlingrService
                        if(!addedHttpElements && currentClass.getInterfaces().stream().map(Object::toString).anyMatch(s -> IHttpService.class.getCanonicalName().equals(s))){
                            AnnotationsUtils.note(currentClass, "Processing HTTP elements > "+canonicalName);
                            classService.registerCodeGenerator(builder -> builder.addCode("\n").addComment("HTTP functions and events are initialized."));

                            currentClass.getEnclosedElements().forEach(element -> {
                                final String name = element.getSimpleName().toString();
                                if(element.getKind() == ElementKind.METHOD) {
                                    switch (name) {
                                        case "defaultGetRequest": {
                                            registerDefaultFunction(classService.getFunctionPrefix()+"get", "GET", classService, element, name, IHttpService.class);
                                            break;
                                        }
                                        case "defaultPostRequest": {
                                            registerDefaultFunction(classService.getFunctionPrefix()+"post", "POST", classService, element, name, IHttpService.class);
                                            break;
                                        }
                                        case "defaultPutRequest": {
                                            registerDefaultFunction(classService.getFunctionPrefix()+"put", "PUT", classService, element, name, IHttpService.class);
                                            break;
                                        }
                                        case "defaultDeleteRequest": {
                                            registerDefaultFunction(classService.getFunctionPrefix()+"delete", "DELETE", classService, element, name, IHttpService.class);
                                            break;
                                        }
                                        case "defaultHeadRequest": {
                                            registerDefaultFunction(classService.getFunctionPrefix()+"head", "HEAD", classService, element, name, IHttpService.class);
                                            break;
                                        }
                                        case "defaultPatchRequest": {
                                            registerDefaultFunction(classService.getFunctionPrefix()+"patch", "PATCH", classService, element, name, IHttpService.class);
                                            break;
                                        }
                                        case "defaultOptionsRequest": {
                                            registerDefaultFunction(classService.getFunctionPrefix()+"options", "OPTIONS", classService, element, name, IHttpService.class);
                                            break;
                                        }
                                        case "defaultWebhookProcessor": {
                                            registerDefaultWebService("WEBHOOK", classService, element, "/", RestMethod.all(), IHttpService.class);
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
                    AnnotationsUtils.error(sServiceElement, e.getMessage());
                    return true;
                }
            } else {
                AnnotationsUtils.error(sServiceElement, "[@%s] is used on [%s]. The annotation only can be used on classes.", sServiceElement.getSimpleName());
                return true;
            }
        }

        // generate the runner that connects the @SlingrService to the framework
        if(!generatedRunner) {
            try {
                if (slingrClassService != null) {
                    AnnotationsUtils.note(slingrClassService.getTypeElement(), "Generating runner for service [%s]", slingrClassService.getTypeElement().getSimpleName());
                    slingrClassService.generateRunner(elementUtils, filer);

                    generatedRunner = true;
                    return false;
                } else {
                    AnnotationsUtils.error(null , "Service [@%s] not found", SlingrService.class.getSimpleName());
                }
            } catch (Exception e) {
                AnnotationsUtils.error(slingrClassService != null ? slingrClassService.getTypeElement() : null, "Exception when generates runner [%s]: %s", e.getClass(), e.getMessage());
            }
        }
        return true; // allow others to process this annotation type
    }

    /**
     * Register a default implementation of a predefined function
     *
     * @param serviceFunction name of the function
     * @param functionLabel label of the function
     * @param serviceClass class used to register the function
     * @param element element to process
     * @param elementName element name
     * @param methodClass class that implements the method
     */
    private void registerDefaultFunction(String serviceFunction, String functionLabel, ClassService serviceClass, Element element, String elementName, Class<?> methodClass) {
        AnnotationsUtils.note(element, "- default %s function [%s]", functionLabel, elementName);
        if(!serviceClass.isFunctionRegistered(serviceFunction)) {
            final ClassFunction function = new ClassFunction(element, serviceFunction, methodClass, true);
            AnnotationsUtils.note(element, "  - processor [%s]", function.getName());
            serviceClass.registerFunction(function);
        } else {
            AnnotationsUtils.note(element, "  - %s function processor is already registered", functionLabel);
        }
    }

    /**
     * Register a default implementation of a predefined web service
     *
     * @param webServiceLabel label of the web service
     * @param serviceClass class used to register the function
     * @param element element to process
     * @param processorPath path of the web service
     * @param processorMethods HTTP methods of the web service
     * @param methodClass class that implements the method
     */
    private void registerDefaultWebService(String webServiceLabel, ClassService serviceClass, Element element, String processorPath, List<RestMethod> processorMethods, Class<?> methodClass) {
        final String path = ClassWebService.normalizePath(processorPath);
        final List<RestMethod> methods;
        if(processorMethods == null || processorMethods.isEmpty()){
            methods = RestMethod.all();
        } else {
            methods = processorMethods;
        }

        for (RestMethod method : methods) {
            registerDefaultWebService(webServiceLabel, serviceClass, element, path, method, methodClass);
        }
    }

    /**
     * Register a default implementation of a predefined web service
     *
     * @param webServiceLabel label of the web service
     * @param serviceClass class used to register the function
     * @param element element to process
     * @param processorPath path of the web service
     * @param processorMethod HTTP method of the web service
     * @param methodClass class that implements the method
     */
    private void registerDefaultWebService(String webServiceLabel, ClassService serviceClass, Element element, String processorPath, RestMethod processorMethod, Class<?> methodClass) {
        AnnotationsUtils.note(element, "- default %s web service [%s %s]", webServiceLabel, processorMethod, processorPath);
        if(!serviceClass.isWebServiceRegistered(processorPath, processorMethod)) {
            final ClassWebService webService = new ClassWebService(element, processorPath, new RestMethod[]{processorMethod}, methodClass, true);
            AnnotationsUtils.note(element, "  - processor [%s]", webService.getName());
            serviceClass.registerWebService(webService);
        } else {
            AnnotationsUtils.note(element, "  - %s web service processor is already registered", webServiceLabel);
        }
    }

    /**
     * Registers each element in the service element on the class service instance
     *
     * @param processor the annotation processing tool framework
     * @param serviceElement element to process
     * @param classService class used to register the elements
     */
    private void processElements(RoundEnvironment processor, Element serviceElement, ClassService classService) {
        final String simpleName = classService.getSimpleName();

        AnnotationsUtils.note(serviceElement, "Processing config on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceConfiguration.class).stream()
                .filter(sPropertyElement -> sPropertyElement.getEnclosingElement().equals(serviceElement))
                .filter(sPropertyElement -> sPropertyElement.getKind() == ElementKind.FIELD)
                .forEach(sPropertyElement -> {
                    AnnotationsUtils.note(sPropertyElement, "- Service configuration [%s]", sPropertyElement.toString());
                    final ClassProperty property = new ClassProperty(sPropertyElement, true);
                    classService.registerProperty(property);
                });
        AnnotationsUtils.note(serviceElement, "Config processed on [%s]: [%s]", simpleName, classService.getPropertiesNames());

        AnnotationsUtils.note(serviceElement, "Processing fields on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceProperty.class).stream()
                .filter(sConfigElement -> sConfigElement.getEnclosingElement().equals(serviceElement))
                .filter(sConfigElement -> sConfigElement.getKind() == ElementKind.FIELD)
                .forEach(sConfigElement -> {
                    AnnotationsUtils.note(sConfigElement, "- ClassProperty [%s]", sConfigElement.toString());
                    final ClassProperty property = new ClassProperty(sConfigElement, false);
                    classService.registerProperty(property);
                });
        AnnotationsUtils.note(serviceElement, "Fields processed on [%s]: [%s]", simpleName, classService.getPropertiesNames());

        AnnotationsUtils.note(serviceElement, "Processing data stores on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceDataStore.class).stream()
                .filter(sDataStoreElement -> sDataStoreElement.getEnclosingElement().equals(serviceElement))
                .filter(sDataStoreElement -> sDataStoreElement.getKind() == ElementKind.FIELD)
                .forEach(sDataStoreElement -> {
                    AnnotationsUtils.note(sDataStoreElement, "- Data store [%s]", sDataStoreElement.toString());
                    final ClassDataStore dataStore = new ClassDataStore(sDataStoreElement);
                    classService.registerDataStore(dataStore);
                });
        AnnotationsUtils.note(serviceElement, "Data stores processed on [%s]: [%s]", simpleName, classService.getDataStoreNames());

        AnnotationsUtils.note(serviceElement, "Processing user data stores on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceUserDataStore.class).stream()
                .filter(sUserDataStoreElement -> sUserDataStoreElement.getEnclosingElement().equals(serviceElement))
                .filter(sUserDataStoreElement -> sUserDataStoreElement.getKind() == ElementKind.FIELD)
                .forEach(sUserDataStoreElement -> {
                    AnnotationsUtils.note(sUserDataStoreElement, "- User data store [%s]", sUserDataStoreElement.toString());
                    final ClassUserDataStore userDataStore = new ClassUserDataStore(sUserDataStoreElement);
                    classService.registerUserDataStore(userDataStore);
                });
        AnnotationsUtils.note(serviceElement, "User data stores processed on [%s]: [%s]", simpleName, classService.getUserDataStoreNames());

        AnnotationsUtils.note(serviceElement, "Processing app loggers on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ApplicationLogger.class).stream()
                .filter(sLoggerElement -> sLoggerElement.getEnclosingElement().equals(serviceElement))
                .filter(sLoggerElement -> sLoggerElement.getKind() == ElementKind.FIELD)
                .forEach(sLoggerElement -> {
                    AnnotationsUtils.note(sLoggerElement, "- App logger [%s]", sLoggerElement.toString());
                    final ClassApplicationLogger appLogger = new ClassApplicationLogger(sLoggerElement);
                    classService.registerAppLogger(appLogger);
                });
        AnnotationsUtils.note(serviceElement, "App loggers processed on [%s]: [%s]", simpleName, classService.getAppLoggerNames());

        AnnotationsUtils.note(serviceElement, "Processing functions on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceFunction.class).stream()
                .filter(sFunctionElement -> sFunctionElement.getEnclosingElement().equals(serviceElement))
                .filter(sFunctionElement -> sFunctionElement.getKind() == ElementKind.METHOD)
                .forEach(sFunctionElement -> {
                    AnnotationsUtils.note(sFunctionElement, "- Function [%s]", sFunctionElement.toString());
                    final ClassFunction function = new ClassFunction(sFunctionElement, false);
                    AnnotationsUtils.note(sFunctionElement, "  - processor [%s]", function.getName());
                    classService.registerFunction(function);
                });
        processor.getElementsAnnotatedWith(ServiceFunctions.class).stream()
                .filter(sFunctionElement -> sFunctionElement.getEnclosingElement().equals(serviceElement))
                .filter(sFunctionElement -> sFunctionElement.getKind() == ElementKind.METHOD)
                .forEach(sFunctionElement -> {
                    AnnotationsUtils.note(sFunctionElement, "- Function (multiple) [%s]", sFunctionElement.toString());
                    for (ServiceFunction serviceFunction : sFunctionElement.getAnnotationsByType(ServiceFunction.class)) {
                        final ClassFunction function = new ClassFunction(sFunctionElement, serviceFunction, false);
                        AnnotationsUtils.note(sFunctionElement, "  - processor [%s]", function.getName());
                        classService.registerFunction(function);
                    }
                });
        AnnotationsUtils.note(serviceElement, "Functions processed on [%s]: [%s]", simpleName, classService.getFunctionsNames());

        AnnotationsUtils.note(serviceElement, "Processing web services on [%s]", simpleName);
        processor.getElementsAnnotatedWith(ServiceWebService.class).stream()
                .filter(sWebServiceElement -> sWebServiceElement.getEnclosingElement().equals(serviceElement))
                .filter(sWebServiceElement -> sWebServiceElement.getKind() == ElementKind.METHOD)
                .forEach(sWebServiceElement -> {
                    AnnotationsUtils.note(sWebServiceElement, "- Web service [%s]", sWebServiceElement.toString());
                    final ClassWebService webService = new ClassWebService(sWebServiceElement, false);
                    AnnotationsUtils.note(sWebServiceElement, "  - processor [%s]", webService.getName());
                    classService.registerWebService(webService);
                });
        processor.getElementsAnnotatedWith(ServiceWebServices.class).stream()
                .filter(sWebServicesElement -> sWebServicesElement.getEnclosingElement().equals(serviceElement))
                .filter(sWebServicesElement -> sWebServicesElement.getKind() == ElementKind.METHOD)
                .forEach(sWebServicesElement -> {
                    AnnotationsUtils.note(sWebServicesElement, "- Web services (multiple) [%s]", sWebServicesElement.toString());
                    for (ServiceWebService serviceWebService : sWebServicesElement.getAnnotationsByType(ServiceWebService.class)) {
                        final ClassWebService webService = new ClassWebService(sWebServicesElement, serviceWebService, false);
                        AnnotationsUtils.note(sWebServicesElement, "  - processor [%s]", webService.getName());
                        classService.registerWebService(webService);
                    }
                });
        AnnotationsUtils.note(serviceElement, "Web services processed on [%s]: [%s]", simpleName, classService.getWebServicesNames());
    }
}