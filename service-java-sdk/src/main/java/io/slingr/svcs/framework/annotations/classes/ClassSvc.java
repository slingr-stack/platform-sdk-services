package io.slingr.svcs.framework.annotations.classes;

import com.squareup.javapoet.*;
import io.slingr.svcs.Svc;
import io.slingr.svcs.framework.IRunner;
import io.slingr.svcs.framework.RegisteredFunction;
import io.slingr.svcs.framework.RegisteredWebService;
import io.slingr.svcs.framework.annotations.SlingrService;
import io.slingr.svcs.framework.annotations.processor.AnnotationsUtils;
import io.slingr.svcs.services.exchange.ReservedName;
import io.slingr.svcs.services.rest.RestMethod;
import io.slingr.svcs.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.Filer;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that represent the service found in a class annotated by <code>@SlingrService</code>
 *
 * <p>Created by lefunes on 01/11/16.
 */
public class ClassSvc {

    public final static String SE_NAME = SlingrService.class.getSimpleName();

    private static final String _methodMain = "main";
    private static final String _methodStartSvc = "startService";
    private static final String _methodExtractArgument = "extractArgument";
    private static final String _methodCreateSvc = "createService";

    private static final String _generatedClassName = "Runner";
    private static final String _staticLogger = "logger";

    private static final String _commandLineParameterConfigurationFile = "configurationFile";
    private static final String _commandLineParameterDefaultWebServiceUri = "defaultWebServiceUri";

    private static final String _parameterSvc = "serviceInstance";
    private static final String _parameterConfigurationFile = _commandLineParameterConfigurationFile;
    private static final String _parameterDefaultWebServiceUri = _commandLineParameterDefaultWebServiceUri;
    private static final String _parameterArgs = "args";
    private static final String _parameterKey = "key";

    private static final String _literalEquals = "=";
    private static final String _variableSvc = "service";
    private static final String _variableSvcType = "serviceType";
    private static final String _variableConfigurationFile = _parameterConfigurationFile;
    private static final String _variableDefaultWebServiceUri = _parameterDefaultWebServiceUri;
    private static final String _variableField = "field";
    private static final String _variableClazz = "clazz";
    private static final String _variableAccessible = "accessible";
    private static final String _variableEx = "ex";
    private static final String _variableStartMethod = "startMethod";

    private static final String _javaMethodFinishedStart = "finishedStart";
    private static final String _javaMethodInternalRegisterWebService = "internalRegisterWebService";
    private static final String _javaMethodInternalRegisterFunction = "internalRegisterFunction";

    private TypeElement annotatedElement;
    private final String qualifiedName;
    private final String simpleName;

    private final String name;
    private final String functionPrefix;

    private final List<CodeGenerator> codeGenerators = new ArrayList<>();
    private final List<ClassApplicationLogger> appLoggers = new ArrayList<>();
    private final List<ClassProperty> properties = new ArrayList<>();
    private final List<ClassDataStore> dataStores = new ArrayList<>();
    private final List<ClassUserDataStore> userDataStores = new ArrayList<>();
    private final Map<String, ClassFunction> functions = new HashMap<>();
    private final Map<String, ClassWebService> webServices = new HashMap<>();
    private final Map<String, Map<RestMethod, Boolean>> webServiceControl = new HashMap<>();

    public ClassSvc(Element sSvcElement) throws IllegalArgumentException {
        this.annotatedElement = (TypeElement) sSvcElement;
        this.qualifiedName = this.annotatedElement.getQualifiedName().toString();
        this.simpleName = this.annotatedElement.getSimpleName().toString();

        final SlingrService annotation = annotatedElement.getAnnotation(SlingrService.class);
        name = annotation.name();
        functionPrefix = StringUtils.isNotBlank(annotation.functionPrefix()) ? annotation.functionPrefix() : "";

        AnnotationsUtils.checkNotEmpty(name, "name", SE_NAME, "class", qualifiedName);
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public String getSimpleName() {
        return simpleName;
    }

    /**
     * Gets the application logs names registered
     *
     * @return list of names
     */
    public Set<String> getAppLoggerNames() {
        return appLoggers.stream()
                .map(ClassApplicationLogger::getSimpleName)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the properties names registered
     *
     * @return list of names
     */
    public Set<String> getPropertiesNames() {
        return properties.stream()
                .map(ClassProperty::getSimpleName)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the data stores names registered
     *
     * @return list of names
     */
    public Set<String> getDataStoreNames() {
        return dataStores.stream()
                .map(ClassDataStore::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the user data stores names registered
     *
     * @return list of names
     */
    public Set<String> getUserDataStoreNames() {
        return userDataStores.stream()
                .map(ClassUserDataStore::getSimpleName)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the functions names registered
     *
     * @return list of names
     */
    public Set<String> getFunctionsNames() {
        return functions.keySet();
    }

    /**
     * Gets the web services names registered
     *
     * @return list of names
     */
    public Set<String> getWebServicesNames() {
        return webServices.keySet();
    }

    /**
     * Get the id as specified in {@link SlingrService#name()}.
     * return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the functionPrefix as specified in {@link SlingrService#functionPrefix()}.
     *
     * @return the function prefix
     */
    public String getFunctionPrefix() {
        return functionPrefix;
    }

    /**
     * Register a code generator
     *
     * @param codeGenerator code generator
     */
    public void registerCodeGenerator(CodeGenerator codeGenerator){
        if(codeGenerator != null) {
            codeGenerators.add(codeGenerator);
        }
    }

    /**
     * Register the application logger to set on {@code Runner} class
     *
     * @param appLogger application logger class
     */
    public void registerAppLogger(ClassApplicationLogger appLogger){
        if(appLogger != null) {
            appLoggers.add(appLogger);
        }
    }

    /**
     * Register the property to set on {@code Runner} class
     *
     * @param property property class
     */
    public void registerProperty(ClassProperty property){
        if(property != null) {
            properties.add(property);
        }
    }

    /**
     * Register the data store to set on {@code Runner} class
     *
     * @param dataStore data store class
     */
    public void registerDataStore(ClassDataStore dataStore){
        if(dataStore != null) {
            dataStores.add(dataStore);
        }
    }

    /**
     * Register the user data store to set on {@code Runner} class
     *
     * @param userDataStore user data store class
     */
    public void registerUserDataStore(ClassUserDataStore userDataStore){
        if(userDataStore != null) {
            userDataStores.add(userDataStore);
        }
    }

    /**
     * Check if the function is already registered on the class
     *
     * @param name function name
     * @return true if the function is already registered on the application
     */
    public boolean isFunctionRegistered(String name){
        if(StringUtils.isNotBlank(name)) {
            if (functions.containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Register the function to set on {@code Runner} class
     *
     * @param function function class
     */
    public void registerFunction(ClassFunction function){
        if(function != null) {
            if (isFunctionRegistered(function.getName())) {
                throw new IllegalStateException(String.format("Function [%s] is already registered on [%s]", function.getName(), getSimpleName()));
            }
            boolean generated = false;
            if(StringUtils.isNotBlank(getFunctionPrefix())) {
                String name = function.getName();
                if(name.startsWith(getFunctionPrefix())) {
                    name = name.substring(getFunctionPrefix().length());
                }

                if (ReservedName.CONNECT_USER.equals(name)) {
                    functions.put(name, function.clone(name, true));
                    functions.put(getFunctionPrefix() + name, function.clone(getFunctionPrefix() + name, true));

                    generated = true;
                } else if (ReservedName.DISCONNECT_USER.equals(name)) {
                    functions.put(name, function.clone(name, true));
                    functions.put(getFunctionPrefix() + name, function.clone(getFunctionPrefix() + name, true));

                    generated = true;
                }
            }

            if(!generated){
                functions.put(function.getName(), function);
            }
        }
    }

    /**
     * Check if the web service is already registered on the class
     *
     * @param path web service name
     * @param method web service name
     * @return true if the web service is already registered on the application
     */
    public boolean isWebServiceRegistered(String path, RestMethod method){
        if(StringUtils.isNotBlank(path) && method != null) {
            if (webServiceControl.containsKey(path) && Boolean.TRUE.equals(webServiceControl.get(path).get(method))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Register the web service to set on {@code Runner} class
     *
     * @param webService web service class
     */
    public void registerWebService(ClassWebService webService){
        if(webService != null) {
            if (webServices.containsKey(webService.getName())) {
                throw new IllegalStateException(String.format("Web service [%s] is already registered on [%s]", webService.getName(), getSimpleName()));
            }
            final String path = webService.getPath();
            if (!webServiceControl.containsKey(path)) {
                final Map<RestMethod, Boolean> reg = new HashMap<>();
                RestMethod.all().forEach(restMethod -> reg.put(restMethod, false));

                webServiceControl.put(path, reg);
            }

            final List<RestMethod> restMethods = webService.getMethods().isEmpty() ? RestMethod.all() : webService.getMethods();
            restMethods.forEach(restMethod -> {
                if (isWebServiceRegistered(path, restMethod)){
                    throw new IllegalStateException(String.format("Web service [%s] contains [%s][%s] that is already registered on [%s]", webService.getName(), restMethod, path, getSimpleName()));
                }
                webServiceControl.get(path).put(restMethod, true);
            });
            webServices.put(webService.getName(), webService);
        }
    }

    /**
     * The original element that was annotated with @Factory
     */
    public TypeElement getTypeElement() {
        return annotatedElement;
    }

    public void generateRunner(Elements elementUtils, Filer filer) throws Exception {
        // Runner
        final PackageElement pkg = elementUtils.getPackageOf(elementUtils.getTypeElement(annotatedElement.getQualifiedName()));

        final TypeElement svcClass = elementUtils.getTypeElement(annotatedElement.getQualifiedName());
        final TypeName svcClassType = TypeName.get(svcClass.asType());

        /*
        -----------------------------------------------------------
        GENERATED CODE:
        -----------------------------------------------------------
        @Override
        public SampleService startService(final Service svc, final String configurationFile, final String defaultWebServiceUri) throws Exception {

            // start service configuration
            if(service == null) {
                throw new IllegalStateException("Invalid service object");
            }
            svc.configure(configurationFile);

            // check service type
            final String svcType = "sample";
            if(!svcType.equals(svc.definitions().getType())) {
                throw new IllegalStateException(String.format("Invalid the defined service name [%s] on service instead to use the value of definitions [%s]", svcType, svc.definitions().getType()));
            }

            // set web service uri
            if(StringUtils.isNotBlank(defaultWebServiceUri)) {
                svc.properties().setDefaultWebServicesUri(defaultWebServiceUri);
            }

            // configure properties and methods
            boolean accessible;
            Field field;
            final Class clazz = Samplesvc.class;

            // APP LOGGER > FIELD [appLogger]
            try {
                field = clazz.getDeclaredField("appLogger");
                accessible = field.isAccessible();
                field.setAccessible(true);
                field.set(svc, svc.appLogs());
                field.setAccessible(accessible);
            } catch (Exception ex) {
                logger.info("Exception when try to set [svc.appLogs()] on [appLogger]: "+ex.getMessage());
                throw ex;
            }

            // SERVICE CONFIGURATION > FIELD [configuration]
            try {
                field = clazz.getDeclaredField("configuration");
                accessible = field.isAccessible();
                field.setAccessible(true);
                field.set(svc, svc.properties().getSvcConfiguration());
                field.setAccessible(accessible);
            } catch (Exception ex) {
                logger.info("Exception when try to set [svc.properties().getSvcConfiguration()] on [configuration]: "+ex.getMessage());
                throw ex;
            }

            // PROPERTY [token] > FIELD [token]
            try {
                field = clazz.getDeclaredField("token");
                accessible = field.isAccessible();
                field.setAccessible(true);
                field.set(svc, svc.properties().getSvcConfiguration().string("token"));
                field.setAccessible(accessible);
            } catch (Exception ex) {
                logger.info("Exception when try to set [svc.properties().getSvcConfiguration().string(\"token\")] on [token]: "+ex.getMessage());
                throw ex;
            }

            // DATA STORE [test_data_store] > FIELD [dataStore]
            try {
                field = clazz.getDeclaredField("dataStore");
                accessible = field.isAccessible();
                field.setAccessible(true);
                field.set(svc, svc.dataStore("test_data_store"));
                field.setAccessible(accessible);
            } catch (Exception ex) {
                logger.info("Exception when try to set [svc.dataStore(\"test_data_store\")] on [dataStore]: "+ex.getMessage());
                throw ex;
            }

            // FUNCTIONS
            final Method function = Svc.class.getDeclaredMethod("internalRegisterFunction", RegisteredFunction.class, Boolean.class);
            function.setAccessible(true);

            // FUNCTION [irrecoverableError] > JAVA METHOD [irrecoverableError]
            function.invoke(svc, new RegisteredFunction("irrecoverableError", "irrecoverableError", MethodParameterType.REQUEST, FunctionResponseType.JSON, MethodAccessorType.PUBLIC), Boolean.TRUE);
            // ...

            // FUNCTIONS (END)
            function.setAccessible(false);

            // WEB SERVICES
            final Method webService = Svc.class.getDeclaredMethod("internalRegisterWebService", RegisteredWebService.class);
            webService.setAccessible(true);

            // WEB SERVICE [/response/] > JAVA METHOD [getResponse]
            webService.invoke(svc, new RegisteredWebService("/response~ge", RestMethod.GET, "/response/", "getResponse", MethodParameterType.NONE, WebServiceResponseType.RESPONSE, MethodAccessorType.PUBLIC));
            // ....

            // WEB SERVICES (END)
            webService.setAccessible(false);

            // finished start process
            final Method startMethod = Svc.class.getDeclaredMethod("finishedStart");
            startMethod.setAccessible(true);
            startMethod.invoke(svc);
            startMethod.setAccessible(false);
            return (SampleSvc) svc;
        }
        -----------------------------------------------------------
        */
        final MethodSpec.Builder startSvcBuilder = MethodSpec.methodBuilder(_methodStartSvc)
                .addModifiers(Modifier.PUBLIC)
                .returns(svcClassType)
                .addParameter(Svc.class, _parameterSvc, Modifier.FINAL)
                .addParameter(String.class, _parameterConfigurationFile, Modifier.FINAL)
                .addParameter(String.class, _parameterDefaultWebServiceUri, Modifier.FINAL)
                .addException(Exception.class)
                .addAnnotation(Override.class)

                .addCode("\n")
                .addComment("start service configuration")
                .beginControlFlow("if($N == null)", _parameterSvc)
                .addStatement("throw new $T($S)", IllegalStateException.class, "Invalid service object")
                .endControlFlow()
                .beginControlFlow("if(!($N instanceof $T))", _parameterSvc, svcClassType)
                .addStatement("throw new $T($S+$T.class.getName()+$S+$N.getClass().getName()+$S)", IllegalStateException.class, "Invalid service instance: expected [", svcClassType, "], current [", _parameterSvc, "]")
                .endControlFlow()
                .addStatement("final $T $N = ($T) $N", svcClassType, _variableSvc, svcClassType, _parameterSvc)
                .addStatement("$N.configure($N)", _variableSvc, _parameterConfigurationFile)

                .addCode("\n")
                .addComment("check service type")
                .addStatement("final $T $N = $S", String.class, _variableSvcType, getName())
                .beginControlFlow("if(!$N.equals($N.definitions().getType()))", _variableSvcType, _variableSvc)
                .addStatement("throw new $T($T.format($S, $N, $N.definitions().getType()))", IllegalStateException.class, String.class,
                        "Invalid the defined service name [%s] on service instead to use the value of definitions [%s]", _variableSvcType, _variableSvc)
                .endControlFlow()

                .addCode("\n")
                .addComment("set web service uri")
                .beginControlFlow("if($T.isNotBlank($N))", StringUtils.class, _parameterDefaultWebServiceUri)
                .addStatement("$N.properties().setDefaultWebServicesUri($N)", _variableSvc, _parameterDefaultWebServiceUri)
                .endControlFlow()

                .addCode("\n")
                .addComment("configure properties and methods")
                .addStatement("$T accessible", boolean.class)
                .addStatement("$T $N", Field.class, _variableField)
                .addStatement("final $T $N = $T.class", Class.class, _variableClazz, svcClass)
                ;

        // process code generators
        codeGenerators.forEach(codeGenerator -> codeGenerator.generate(startSvcBuilder));

        // process app loggers
        appLoggers.forEach(appLogger -> generateSet(startSvcBuilder, appLogger));

        // process properties
        properties.forEach(properties -> generateSet(startSvcBuilder, properties));

        // process data stores
        dataStores.forEach(dataStore -> generateSet(startSvcBuilder, dataStore));

        // process user data stores
        userDataStores.forEach(dataStore -> generateSet(startSvcBuilder, dataStore));

        // process functions
        if(!functions.isEmpty()) {
            final String registerFunction = "function";
            startSvcBuilder
                    .addCode("\n")
                    .addComment("FUNCTIONS")
                    .addStatement("final $T $N = $T.class.getDeclaredMethod($S, $T.class, $T.class)", Method.class, registerFunction, Svc.class, _javaMethodInternalRegisterFunction, RegisteredFunction.class, Boolean.class)
                    .addStatement("$N.setAccessible(true)", registerFunction);

            functions.forEach((key, classFunction) -> generateRegister(startSvcBuilder, registerFunction, classFunction));

            startSvcBuilder
                    .addCode("\n")
                    .addComment("FUNCTIONS (END)")
                    .addStatement("$N.setAccessible(false)", registerFunction);
        }

        // process web services
        if(!webServices.isEmpty()) {
            final String registerWebService = "webService";
            startSvcBuilder
                    .addCode("\n")
                    .addComment("WEB SERVICES")
                    .addStatement("final $T $N = $T.class.getDeclaredMethod($S, $T.class)", Method.class, registerWebService, Svc.class, _javaMethodInternalRegisterWebService, RegisteredWebService.class)
                    .addStatement("$N.setAccessible(true)", registerWebService);

            webServices.values().forEach(webService -> generateRegister(startSvcBuilder, registerWebService, webService));

            startSvcBuilder
                    .addCode("\n")
                    .addComment("WEB SERVICES (END)")
                    .addStatement("$N.setAccessible(false)", registerWebService);
        }

        startSvcBuilder
                .addCode("\n")
                .addComment("finished start process")
                .addStatement("final $T $N = $T.class.getDeclaredMethod($S)", Method.class, _variableStartMethod, Svc.class, _javaMethodFinishedStart)
                .addStatement("$N.setAccessible(true)", _variableStartMethod)
                .addStatement("$N.invoke($N)", _variableStartMethod, _variableSvc)
                .addStatement("$N.setAccessible(false)", _variableStartMethod)
                .addStatement("return $N", _variableSvc);

        final MethodSpec startSvc = startSvcBuilder.build();

        /*
        -----------------------------------------------------------
        GENERATED CODE:
        -----------------------------------------------------------
        @Override
        public SampleSvc createSvc() {
            return new SampleSvc();
        }
        -----------------------------------------------------------
        */
        final MethodSpec createSvc = MethodSpec.methodBuilder(_methodCreateSvc)
                .addModifiers(Modifier.PUBLIC)
                .returns(svcClassType)
                .addAnnotation(Override.class)

                .addStatement("return new $T()", svcClass)
                .build();

        /*
        -----------------------------------------------------------
        GENERATED CODE:
        -----------------------------------------------------------
        public static void main(final String[] args) throws Exception {
            // extract command line parameters
            final String configurationFile = extractArgument("configurationFile", args);
            final String defaultWebServiceUri = extractArgument("defaultWebServiceUri", args);

            // start service configuration
            new Runner().startSvc(configurationFile, defaultWebServiceUri);
        }
        -----------------------------------------------------------
        */
        final MethodSpec main = MethodSpec.methodBuilder(_methodMain)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID)
                .addParameter(String[].class, _parameterArgs, Modifier.FINAL)
                .addException(Exception.class)
                .addJavadoc("Entry point of the service\n\n")
                .addJavadoc("@param $N command line arguments.\n", _parameterArgs)

                .addComment("extract command line parameters")
                .addStatement("final $T $N = $N($S, $N)", String.class, _variableConfigurationFile, _methodExtractArgument, _commandLineParameterConfigurationFile, _parameterArgs)
                .addStatement("final $T $N = $N($S, $N)", String.class, _variableDefaultWebServiceUri, _methodExtractArgument, _commandLineParameterDefaultWebServiceUri, _parameterArgs)

                .addCode("\n")
                .addComment("start service configuration")
                .addStatement("new $N().$N($N, $N)", _generatedClassName, startSvc, _variableConfigurationFile, _variableDefaultWebServiceUri)
                .build();

        final TypeSpec runnerClass = TypeSpec.classBuilder(_generatedClassName)
                .superclass(IRunner.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Class used to initialize and run the service\n\n")
                .addJavadoc("<p>Created by <b>Slingr</b> on $N.\n", DateFormat.getDateInstance().format(System.currentTimeMillis()))
                .addField(FieldSpec
                        .builder(Logger.class, _staticLogger, Modifier.STATIC, Modifier.FINAL, Modifier.PRIVATE)
                        .initializer("$T.getLogger($T.class)", LoggerFactory.class, Svc.class)
                        .build()
                )
                .addMethod(main)
                .addMethod(createSvc)
                .addMethod(startSvc)
                .build();

        // create runner file
        final JavaFileObject jfo = filer.createSourceFile(_generatedClassName);
        final Writer writer = jfo.openWriter();

        final JavaFile javaFile = JavaFile.builder(pkg.getQualifiedName().toString(), runnerClass).build();
        javaFile.writeTo(writer);

        writer.flush();
        writer.close();
    }

    /**
     * Sets the value on the property using the accessor found for this one
     *
     * @param builder method builder
     * @param property property to set
     * @param value value to set on property
     */
    private static void generateSetToProperty(MethodSpec.Builder builder, SettableProperty property, String value){
        builder.beginControlFlow("try");

        if(property.getAccessorType() == AccessorType.REFLECTION){
            // sets the property using reflection
            builder.addStatement("$N = $N.getDeclaredField($S)", _variableField, _variableClazz, property.getAccessor())
                    .addStatement("$N = $N.isAccessible()", _variableAccessible, _variableField)
                    .addStatement("$N.setAccessible(true)", _variableField)
                    .addStatement("$N.set($N, $N)", _variableField, _variableSvc, value)
                    .addStatement("$N.setAccessible($N)", _variableField, _variableAccessible);
        } else {
            if (property.getAccessorType() == AccessorType.SETTER) {
                // sets the property using a setter
                builder.addStatement("$N.$N($N)", _variableSvc, property.getAccessor(), value);
            } else {
                // sets the property using the public property
                builder.addStatement("$N.$N = $N", _variableSvc, property.getAccessor(), value);
            }
        }

        builder.nextControlFlow("catch ($T $N)", Exception.class, _variableEx)
                .addStatement("$N.info($S+$N.getMessage())", _staticLogger, String.format("Exception when try to set [%s] on [%s]: ", value, property.getAccessor()), _variableEx)
                .addStatement("throw $N", _variableEx)
                .endControlFlow();
    }

    /**
     * Generates code to set an application logger on the method builder
     *
     * @param builder method builder
     * @param appLogger logger to set
     */
    private static void generateSet(MethodSpec.Builder builder, ClassApplicationLogger appLogger){
        if(appLogger != null){
            builder.addCode("\n").addComment("APP LOGGER > FIELD [$N]", appLogger.getSimpleName());
            generateSetToProperty(builder, appLogger, String.format("%s.appLogs()", _variableSvc));
        }
    }

    /**
     * Generates code to set a property on the method builder
     *
     * @param builder method builder
     * @param property property to set
     */
    private static void generateSet(MethodSpec.Builder builder, ClassProperty property){
        if(property != null){
            if(property.isSvcConfiguration()){
                builder.addCode("\n").addComment("SERVICE CONFIGURATION > FIELD [$N]", property.getSimpleName());
            } else {
                builder.addCode("\n").addComment("PROPERTY [$N] > FIELD [$N]", property.getName(), property.getSimpleName());
            }

            final CodeBlock.Builder code = CodeBlock.builder()
                    .add("$N.properties().getSvcConfiguration()", _variableSvc);

            if(!property.getType().equals(PropertyType.CONFIG)) {

                if (property.getType().equals(PropertyType.BOOLEAN)) {
                    code.add(".is");
                } else if (property.getType().equals(PropertyType.JSON)) {
                    code.add(".json");
                } else {
                    code.add(".string");
                }

                code.add("($S", property.getName());

                if (property.getDefaultValue() != null) {
                    code.add(", ");

                    if (property.getType().equals(PropertyType.BOOLEAN)) {
                        code.add("$N", property.getDefaultValue());
                    } else if (property.getType().equals(PropertyType.JSON)) {
                        code.add("$T.parse($S)", Json.class, property.getDefaultValue());
                    } else {
                        code.add("$S", property.getDefaultValue());
                    }
                }
                code.add(")");
            }

            generateSetToProperty(builder, property, code.build().toString());
        }
    }

    /**
     * Generates code to set a data store on the method builder
     *
     * @param builder method builder
     * @param dataStore data store to set
     */
    private static void generateSet(MethodSpec.Builder builder, ClassDataStore dataStore){
        if(dataStore != null){
            builder.addCode("\n").addComment("DATA STORE [$N] > FIELD [$N]", dataStore.getName(), dataStore.getSimpleName());
            generateSetToProperty(builder, dataStore, String.format("%s.dataStore(\"%s\")", _variableSvc, dataStore.getName()));
        }
    }

    /**
     * Generates code to set a data store on the method builder
     *
     * @param builder method builder
     * @param userDataStore user data store to set
     */
    private static void generateSet(MethodSpec.Builder builder, ClassUserDataStore userDataStore){
        if(userDataStore != null){
            builder.addCode("\n").addComment("USER DATA STORE > FIELD [$N]", userDataStore.getSimpleName());
            generateSetToProperty(builder, userDataStore, String.format("%s.userDataStore()", _variableSvc));
        }
    }

    /**
     * Generates code to register a function on the method builder
     *
     * @param builder method builder
     * @param registerFunction name of the java method used to register functions
     * @param function function to register
     */
    private static void generateRegister(MethodSpec.Builder builder, String registerFunction, ClassFunction function){
        if(function != null){
            builder.addCode("\n").addComment("FUNCTION [$N] > JAVA METHOD [$N]", function.getName(), function.getSimpleName());
            if(function.getMethodClass() == null) {
                builder.addStatement("$N.invoke($N, new $T($S, $S, $T.$N, $T.$N, $T.$N), $T.$N)", registerFunction, _variableSvc, RegisteredFunction.class,
                        function.getName(), function.getSimpleName(),
                        MethodParameterType.class, function.getParameterType().name(),
                        FunctionResponseType.class, function.getResponseType().name(),
                        MethodAccessorType.class, function.getAccessorType().name(),
                        Boolean.class, (""+!function.isGenerated()).toUpperCase()
                );
            } else {
                builder.addStatement("$N.invoke($N, new $T($S, $S, $T.$N, $T.$N, $T.$N, $T.class), $T.$N)", registerFunction, _variableSvc, RegisteredFunction.class,
                        function.getName(), function.getSimpleName(),
                        MethodParameterType.class, function.getParameterType().name(),
                        FunctionResponseType.class, function.getResponseType().name(),
                        MethodAccessorType.class, function.getAccessorType().name(),
                        function.getMethodClass(),
                        Boolean.class, (""+!function.isGenerated()).toUpperCase()
                );
            }
        }
    }

    /**
     * Generates code to register a web service on the method builder
     *
     * @param builder method builder
     * @param registerWebService name of the java method used to register functions
     * @param webService web service to register
     */
    private static void generateRegister(MethodSpec.Builder builder, String registerWebService, ClassWebService webService){
        if(webService != null){
            builder.addCode("\n").addComment("WEB SERVICE [$N] > JAVA METHOD [$N]", webService.getPath(), webService.getSimpleName());

            if(webService.getMethodClass() == null) {
                for (RestMethod method : webService.getMethods()) {
                    builder.addStatement("$N.invoke($N, new $T($S, $T.$N, $S, $S, $T.$N, $T.$N, $T.$N))", registerWebService, _variableSvc, RegisteredWebService.class,
                            ClassWebService.generateName(webService.getPath(), method.toInitials()),
                            RestMethod.class, method.name(),
                            webService.getPath(), webService.getSimpleName(),
                            MethodParameterType.class, webService.getParameterType().name(),
                            WebServiceResponseType.class, webService.getResponseType().name(),
                            MethodAccessorType.class, webService.getAccessorType().name()
                    );
                }
            } else {
                for (RestMethod method : webService.getMethods()) {
                    builder.addStatement("$N.invoke($N, new $T($S, $T.$N, $S, $S, $T.$N, $T.$N, $T.$N, $T.class))", registerWebService, _variableSvc, RegisteredWebService.class,
                            ClassWebService.generateName(webService.getPath(), method.toInitials()),
                            RestMethod.class, method.name(),
                            webService.getPath(), webService.getSimpleName(),
                            MethodParameterType.class, webService.getParameterType().name(),
                            WebServiceResponseType.class, webService.getResponseType().name(),
                            MethodAccessorType.class, webService.getAccessorType().name(),
                            webService.getMethodClass()
                    );
                }
            }
        }
    }

    /**
     * Check if the Service class defined contains a valid format
     */
    public boolean isValidClass() {
        if (!annotatedElement.getModifiers().contains(Modifier.PUBLIC)) {
            AnnotationsUtils.error(annotatedElement, "The class [%s] is not public.", qualifiedName);
            return false;
        }

        // Check if it's an abstract class
        if (annotatedElement.getModifiers().contains(Modifier.ABSTRACT)) {
            AnnotationsUtils.error(annotatedElement, "The class [%s] is abstract. You can't annotate abstract classes with @%s", qualifiedName, SE_NAME);
            return false;
        }

        // Check inheritance: Class must be a subclass of Service
        TypeElement currentClass = annotatedElement;
        while (true) {
            TypeMirror superClassType = currentClass.getSuperclass();

            if (superClassType.getKind() == TypeKind.NONE) {
                // Basis class (java.lang.Object) reached, so exit
                AnnotationsUtils.error(annotatedElement, "The class [%s] annotated with @%s must inherit from [%s]", qualifiedName, SE_NAME, Svc.class.getCanonicalName());
                return false;
            }

            if (superClassType.toString().equals(Svc.class.getCanonicalName())) {
                // Required super class found
                break;
            }

            // Moving up in inheritance tree
            currentClass = (TypeElement) AnnotationsUtils.asElement(superClassType);
        }

        // Check if an empty public constructor is given
        boolean foundConstructor = false;
        for (Element enclosed : annotatedElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructorElement = (ExecutableElement) enclosed;
                if (constructorElement.getParameters().size() == 0 && constructorElement.getModifiers().contains(Modifier.PUBLIC)) {
                    // Found an empty constructor
                    foundConstructor = true;
                    break;
                }
            }
        }
        if(!foundConstructor) {
            // No empty constructor found
            AnnotationsUtils.error(annotatedElement, "The class [%s] must provide an public empty default constructor", qualifiedName);
            return false;
        }
        return true;
    }
}