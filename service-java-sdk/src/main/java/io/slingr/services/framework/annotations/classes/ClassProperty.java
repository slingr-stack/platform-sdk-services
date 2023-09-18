package io.slingr.services.framework.annotations.classes;

import io.slingr.services.framework.annotations.ServiceProperty;
import io.slingr.services.framework.annotations.processor.AnnotationsUtils;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * Class that represent the property (a field annotated by <code>@ServiceProperty</code>) found inside a class annotated
 * by <code>@SlingrService</code>
 *
 */
public class ClassProperty implements SettableProperty {
    public final static String SP_NAME = ServiceProperty.class.getSimpleName();
    public final static String NULL_DEFAULT_VALUE = "----NULL----";

    private final String simpleName;

    private final String name;
    private PropertyType type;
    private final String defaultValue;

    private final AccessorType accessorType;
    private final String accessor;
    private final boolean isServiceConfiguration;

    public ClassProperty(Element sPropertyElement, boolean isServiceConfiguration) throws IllegalArgumentException {
        this(sPropertyElement, sPropertyElement.getAnnotation(ServiceProperty.class), isServiceConfiguration);
    }

    private ClassProperty(Element sPropertyElement, ServiceProperty annotation, boolean isServiceConfiguration) throws IllegalArgumentException {
        this(sPropertyElement, annotation != null ? annotation.name() : null, annotation != null ? annotation.defaultValue() : null, isServiceConfiguration);
    }

    public ClassProperty(Element sPropertyElement, String propertyName, String propertyDefaultValue, boolean isServiceConfiguration) throws IllegalArgumentException {
        this.isServiceConfiguration = isServiceConfiguration;

        VariableElement annotatedElement = (VariableElement) sPropertyElement;
        this.simpleName = annotatedElement.getSimpleName().toString().trim();

        name = StringUtils.isNotBlank(propertyName) ? propertyName : simpleName;
        AnnotationsUtils.checkNotEmpty(name, "name", SP_NAME, "property", name);

        final String setterName = String.format("set%S%s", simpleName.substring(0,1), simpleName.length() > 1 ? simpleName.substring(1) : "");
        type = AnnotationsUtils.isValidSetter(null, annotatedElement, simpleName, setterName, this.isServiceConfiguration);
        if (!this.isServiceConfiguration && (type == PropertyType.BOOLEAN || type == PropertyType.STRING || type == PropertyType.JSON)){
            // valid setter
            accessorType = AccessorType.SETTER;
            accessor = setterName;
        } else if (this.isServiceConfiguration && type == PropertyType.CONFIG){
            // valid config setter
            accessorType = AccessorType.SETTER;
            accessor = setterName;
        } else {
            // check property
            accessor = simpleName;

            type = AnnotationsUtils.isValidProperty(null, annotatedElement, simpleName, accessor, this.isServiceConfiguration);
            if (type == null || type == PropertyType.OTHER){
                throw new IllegalStateException(String.format("ServiceProperty [%s] is not valid", getName()));
            }

            if (annotatedElement.getModifiers().contains(Modifier.PRIVATE) || annotatedElement.getModifiers().contains(Modifier.PROTECTED)) {
                accessorType = AccessorType.REFLECTION;
            } else {
                // property is accessible
                accessorType = AccessorType.PROPERTY;
            }
        }
        AnnotationsUtils.checkNotNull(type, "type", SP_NAME, "property", name);

        String dv = StringUtils.isNotBlank(propertyDefaultValue) ? propertyDefaultValue : "";
        if(dv.equals(NULL_DEFAULT_VALUE)){
            dv = null;
        } else if(type == PropertyType.BOOLEAN){
            dv = ""+Boolean.parseBoolean(dv);
        }
        defaultValue = dv;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getAccessor() {
        return accessor;
    }

    @Override
    public AccessorType getAccessorType() {
        return accessorType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public PropertyType getType() {
        return type;
    }

    /**
     * True if the property is an Service Configuration type
     *
     * @return true if the property is an Service Configuration type
     */
    public boolean isServiceConfiguration() {
        return isServiceConfiguration;
    }
}
