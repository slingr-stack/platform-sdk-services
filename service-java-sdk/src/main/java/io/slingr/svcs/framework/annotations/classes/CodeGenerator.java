package io.slingr.svcs.framework.annotations.classes;


import com.squareup.javapoet.MethodSpec;

/**
 * Implements this class to add code on the main method of Service runner.
 *
 * <p>Created by lefunes on 06/04/18.
 */
public interface CodeGenerator {
    /**
     * Generates the code on the method
     *
     * @param builder method builder
     */
    void generate(MethodSpec.Builder builder);
}
