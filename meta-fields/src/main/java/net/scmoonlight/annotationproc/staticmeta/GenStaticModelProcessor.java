package net.scmoonlight.annotationproc.staticmeta;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The main Annotation Processor.
 * Will process files annotated with @GenStaticModel
 * @see net.scmoonlight.annotationproc.staticmeta.GenStaticModel
 */
@SupportedAnnotationTypes("net.scmoonlight.annotationproc.staticmeta.GenStaticModel")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions({"metaFieldsSuffix", "metaFieldsVerbose"})
public class GenStaticModelProcessor extends AbstractProcessor {
    /**
     * The time that will be added to the @Generated annotation in the generated class
     */
    private final String now = OffsetDateTime.now().toString();


    /**
     *
     * @param annotations the annotation interfaces requested to be processed
     * @param roundEnv  environment for information about the current and prior round
     * @return false, to allow further processing to occur.
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Short circuit if the annotation is not the one we care about.
        if (annotations.stream().map(Objects::toString).noneMatch(GenStaticModel.class.getName()::equals)) {
            return false;
        }

        try {
            for (TypeElement annotation : annotations) {
                generateMetaClassFromAnnotation(roundEnv, annotation, getClassSuffix());
            }
        } catch (IOException ex) {
            error("Could not process annotations: " + annotations + " " + ex.getMessage());
            return false;
        }
        return false;
    }

    /**
     * Return the annotation processor option value from the processing environment
     * @param key the option to be retrieved
     * @return the value of the option or null
     */
    protected String getOption(String key) {
        return processingEnv.getOptions().get(key);
    }

    /**
     * Return the suffix that should be used for the generated class.
     * Defaults to 'Meta'
     * @return Meta or value of -AmetaFieldsSuffix=.... annotation processor configuration
     */
    private String getClassSuffix() {
        return Optional.ofNullable(getOption("metaFieldsSuffix")).orElse("Meta");
    }

    /**
     * For each annotated element in the environment, generate a new source file
     * @param roundEnv the compilation environment
     * @param annotation the marker annotation we care about
     * @param suffix Suffix for the generated source
     * @throws IOException
     */
    private void generateMetaClassFromAnnotation(RoundEnvironment roundEnv, TypeElement annotation, String suffix) throws IOException {
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
        for (Element e : annotatedElements) {
            log("found annotated element "+e);
            Names names = getNames(e, suffix);
            JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(names.pkg + '.' + names.metaCls, e);
            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                writeClassDefinition(e, names, now, out);
            }
        }
    }

    /**
     * The core work.  For the source element, write out the new meta class definition.
     * @param e the source class element
     * @param names the record of names for generation
     * @param now the generation time
     * @param out the PrintWriter for the generated class
     */
    private void writeClassDefinition(Element e, Names names, String now, PrintWriter out) {
        String processorName = getClass().getName();
        String openTemplate = """
                package %s;

                import net.scmoonlight.annotationproc.staticmeta.StaticModel;
                import java.util.List;
                import javax.annotation.processing.Generated;

                @Generated(value = "%s", date = "%s")
                @StaticModel(%s.class)
                public final class %s {
                  private %s() {}
                """.formatted(names.pkg, processorName, now, names.cls, names.metaCls, names.metaCls);
        out.println(openTemplate);
        List<? extends Element> enclosedElements = e.getEnclosedElements().stream().filter(ee -> ee.getKind().isField()).toList();
        List<String> allNames = enclosedElements.stream().map(Element::getSimpleName).map(Objects::toString).toList();
        log("found field names : "+allNames);
        // For each field, write out that property name as a field
        for (String n : allNames) {
            out.println("  public static final String %s = \"%s\";".formatted(n, n));
        }
        // create an "allFields" list of all the field names
        out.println("  public static final List<String> allFields = List.of(%s);".formatted(String.join(",", allNames)));
        out.println("}");
    }

    /**
     * A
     * @param fqn Fully qualified class name
     * @param pkg Package for the class
     * @param cls Simple class name
     * @param metaCls Simple class name for the generated class
     */
    private record Names(String fqn, String pkg, String cls, String metaCls) { }

    /**
     * Create simple class to hold the class name parts
     * @param e the class being processed
     * @param suffix the suffix for the generated class
     * @return Names record with values
     */
    private Names getNames(Element e, String suffix) {
        Elements elementUtils = processingEnv.getElementUtils();
        String className = ((TypeElement) e).getQualifiedName().toString();
        String packageName = elementUtils.getPackageOf(e).toString();
        String simpleClassName = e.getSimpleName().toString();
        return new Names(className, packageName, simpleClassName, simpleClassName + suffix);
    }

    /**
     * If the -AmetaFieldsVerbose option is set, then log some progress
     * @param msg
     */
    private void log(String msg) {
        if (getOption("metaFieldsVerbose")!=null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
        }
    }

    /**
     * Log the error message to the annotation processor error writer
     * @param msg
     */
    private void error(String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg);
    }
}
