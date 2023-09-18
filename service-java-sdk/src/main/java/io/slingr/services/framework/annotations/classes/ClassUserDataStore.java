package io.slingr.services.framework.annotations.classes;

import io.slingr.services.framework.annotations.processor.AnnotationsUtils;
import io.slingr.services.services.datastores.DataStore;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * Class that represent the user data stores (a field annotated by <code>@ServiceUserDataStore</code>) found inside a
 * class annotated by <code>@SlingrService</code>
 *
 */
public class ClassUserDataStore implements SettableProperty {
    private final String simpleName;

    private final AccessorType accessorType;
    private final String accessor;

    public ClassUserDataStore(Element sDataStoreElement) throws IllegalArgumentException {
        VariableElement annotatedElement = (VariableElement) sDataStoreElement;
        this.simpleName = annotatedElement.getSimpleName().toString().trim();

        final String setterName = String.format("set%S%s", simpleName.substring(0,1), simpleName.length() > 1 ? simpleName.substring(1) : "");
        PropertyType type = AnnotationsUtils.isValidSetter(DataStore.class, annotatedElement, simpleName, setterName);
        if (type != null){
            // valid setter
            accessorType = AccessorType.SETTER;
            accessor = setterName;
        } else {
            // check property
            accessor = simpleName;

            type = AnnotationsUtils.isValidProperty(DataStore.class, annotatedElement, simpleName, accessor);
            if (type == null){
                throw new IllegalStateException(String.format("User data Store [%s] is not valid", simpleName));
            }

            if (annotatedElement.getModifiers().contains(Modifier.PRIVATE) || annotatedElement.getModifiers().contains(Modifier.PROTECTED)) {
                accessorType = AccessorType.REFLECTION;
            } else {
                // property is accessible
                accessorType = AccessorType.PROPERTY;
            }
        }
    }

    public String getSimpleName() {
        return simpleName;
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