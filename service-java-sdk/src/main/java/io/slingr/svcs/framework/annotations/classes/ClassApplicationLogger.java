package io.slingr.svcs.framework.annotations.classes;

import io.slingr.svcs.framework.annotations.processor.AnnotationsUtils;
import io.slingr.svcs.services.AppLogs;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;

/**
 * Class that represent the app loggers (a field annotated by <code>@ApplicationLogger</code>) found inside a class annotated
 * by <code>@SlingrService</code>
 *
 * <p>Created by lefunes on 17/11/16.
 */
public class ClassApplicationLogger implements SettableProperty {
    private final String simpleName;

    private final AccessorType accessorType;
    private final String accessor;

    public ClassApplicationLogger(Element sAppLoggerElement) throws IllegalArgumentException {
        final VariableElement annotatedElement = (VariableElement) sAppLoggerElement;
        this.simpleName = annotatedElement.getSimpleName().toString().trim();

        final String setterName = String.format("set%S%s", simpleName.substring(0,1), simpleName.length() > 1 ? simpleName.substring(1) : "");
        PropertyType type = AnnotationsUtils.isValidSetter(AppLogs.class, annotatedElement, simpleName, setterName);
        if (type != null){
            // valid setter
            accessorType = AccessorType.SETTER;
            accessor = setterName;
        } else {
            // check property
            accessor = simpleName;

            type = AnnotationsUtils.isValidProperty(AppLogs.class, annotatedElement, simpleName, accessor);
            if (type == null){
                throw new IllegalStateException(String.format("App logger [%s] is not valid", simpleName));
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