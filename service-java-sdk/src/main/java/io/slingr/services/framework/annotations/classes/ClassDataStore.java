package io.slingr.services.framework.annotations.classes;

import io.slingr.services.framework.annotations.ServiceDataStore;
import io.slingr.services.framework.annotations.processor.AnnotationsUtils;
import io.slingr.services.services.datastores.DataStore;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * Class that represent the data stores (a field annotated by <code>@ServiceDataStore</code>) found inside a class annotated
 * by <code>@SlingrService</code>
 *
 */
public class ClassDataStore implements SettableProperty {
    public final static String SDS_NAME = ServiceDataStore.class.getSimpleName();

    private final String simpleName;

    private final String name;

    private final AccessorType accessorType;
    private final String accessor;

    public ClassDataStore(Element sDataStoreElement) throws IllegalArgumentException {
        this(sDataStoreElement, sDataStoreElement.getAnnotation(ServiceDataStore.class));
    }

    private ClassDataStore(Element sDataStoreElement, ServiceDataStore annotation) throws IllegalArgumentException {
        this(sDataStoreElement, annotation != null ? annotation.name() : null);
    }

    public ClassDataStore(Element sDataStoreElement, String dataStoreName) throws IllegalArgumentException {
        VariableElement annotatedElement = (VariableElement) sDataStoreElement;
        this.simpleName = annotatedElement.getSimpleName().toString().trim();

        name = StringUtils.isNotBlank(dataStoreName) ? dataStoreName : simpleName;

        AnnotationsUtils.checkNotEmpty(name, "name", SDS_NAME, "data store", name);

        final String setterName = String.format("set%S%s", simpleName.charAt(0), simpleName.length() > 1 ? simpleName.substring(1) : "");
        PropertyType type = AnnotationsUtils.isValidSetter(DataStore.class, annotatedElement, simpleName, setterName);
        if (type == null) {
            // check property
            accessor = simpleName;

            type = AnnotationsUtils.isValidProperty(DataStore.class, annotatedElement, simpleName, accessor);
            if (type == null){
                throw new IllegalStateException(String.format("Data Store [%s] is not valid", simpleName));
            }

            if (annotatedElement.getModifiers().contains(Modifier.PRIVATE) || annotatedElement.getModifiers().contains(Modifier.PROTECTED)) {
                accessorType = AccessorType.REFLECTION;
            } else {
                // property is accessible
                accessorType = AccessorType.PROPERTY;
            }
        } else {
            // valid setter
            accessorType = AccessorType.SETTER;
            accessor = setterName;
        }
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
}