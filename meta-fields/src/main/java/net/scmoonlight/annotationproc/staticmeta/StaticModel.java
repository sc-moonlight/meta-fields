package net.scmoonlight.annotationproc.staticmeta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation added to generated Meta classes.  References the original source class.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface StaticModel {
    /**
     * value refers to the original source class.
     * @return the original class
     */
    Class value();
}