package moonlight.annotationproc.staticmeta;

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

@SupportedAnnotationTypes("moonlight.annotationproc.staticmeta.GenStaticModel")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
@SupportedOptions({"metaFieldsSuffix", "metaFieldsVerbose"})
public class GenStaticModelProcessor extends AbstractProcessor {
    private final String now = OffsetDateTime.now().toString();

    record Names(String fqn, String pkg, String cls, String metaCls) {
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        String suffix = Optional.ofNullable(processingEnv.getOptions().get("metaFieldsSuffix")).orElse("Meta");
        // Short circuit if the annotation is not the one we care about.
        if (annotations.stream().map(Objects::toString).noneMatch(GenStaticModel.class.getName()::equals)) {
            return false;
        }

        try {
            for (TypeElement annotation : annotations) {
                generateMetaClassFromAnnotation(roundEnv, annotation, suffix);
            }
        } catch (IOException ex) {
            error("Could not process annotations: " + annotations);
            return false;
        }
        return false;
    }

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

    private void writeClassDefinition(Element e, Names names, String now, PrintWriter out) {
        String processorName = getClass().getName();
        String openTemplate = """
                package %s;

                import moonlight.annotationproc.staticmeta.StaticModel;
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
        for (String n : allNames) {
            out.println("  public static final String %s = \"%s\";".formatted(n, n));
        }
        out.println("  public static final List<String> allFields = List.of(%s);".formatted(String.join(",", allNames)));

        for ( Element ee : enclosedElements) {
            log("ee = "+ ee + "," + ee.asType());
        }
        out.println("}");
    }

    private Names getNames(Element e, String suffix) {
        Elements elementUtils = processingEnv.getElementUtils();
        String className = ((TypeElement) e).getQualifiedName().toString();
        String packageName = elementUtils.getPackageOf(e).toString();
        String simpleClassName = e.getSimpleName().toString();
        return new Names(className, packageName, simpleClassName, simpleClassName + suffix);
    }

    private void log(String msg) {
        if (processingEnv.getOptions().containsKey("metaFieldsVerbose")) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
        }
    }
    private void error(String msg) {
        processingEnv.getMessager().printMessage(Kind.ERROR, msg);
    }
}
