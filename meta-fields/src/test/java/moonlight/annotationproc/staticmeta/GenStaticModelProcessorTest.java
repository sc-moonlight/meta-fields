package moonlight.annotationproc.staticmeta;

import com.sun.source.util.JavacTask;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
public class GenStaticModelProcessorTest {
    private final File testPkgDir = new File("test");

    @BeforeEach
    public void removeFiles() throws IOException {
        FileUtils.deleteDirectory(testPkgDir);
        assertTrue(testPkgDir.mkdir());
    }
    @Test
    public void testFieldNamesProcessor() throws Exception {
        String sourceCode = """
                package test;
                @moonlight.annotationproc.staticmeta.GenStaticModel
                public class TestClass {
                    private String testField;
                    private Integer myIntField;
                }
                """;

        File metaFile = new File(testPkgDir,"TestClassMeta.java");
        assertFalse(metaFile.exists());
        processSourceCode("TestClass.java", sourceCode, Map.of());
        assertTrue(metaFile.exists());
        String metaSource = FileUtils.readFileToString(metaFile, Charset.defaultCharset());
        assertTrue(metaSource.contains("@StaticModel"));
        assertTrue(metaSource.contains("public final class TestClassMeta {"));
        assertTrue(metaSource.contains("public static final String testField = \"testField\""));
        assertTrue(metaSource.contains("public static final String myIntField = \"myIntField\""));
        assertTrue(metaSource.contains("public static final List<String> allFields = List.of(testField,myIntField)"));
    }

    @Test
    public void testNoAnnotationNoOutput() throws Exception {
        String sourceCode = """
                package test;
                public class TestClass {
                    private String testField;
                }
                """;

        File metaFile = new File(testPkgDir,"TestClassMeta.java");
        assertFalse(metaFile.exists());
        processSourceCode("TestClass.java", sourceCode, null);
        assertFalse(metaFile.exists());
    }

    @Test
    public void testFieldNamesProcessorChangeSuffix() throws Exception {
        String sourceCode = """
                package test;
                import moonlight.annotationproc.staticmeta.*;

                @GenStaticModel
                public class TestClass {
                    private String testField;
                }
                """;

        File metaFile = new File(testPkgDir,"TestClass_.java");
        assertFalse(metaFile.exists());
        processSourceCode("TestClass.java", sourceCode, Map.of("metaFieldsSuffix","_"));
        assertTrue(metaFile.exists());
    }

    private void processSourceCode(String classFileName, String sourceCode, Map<String, String> options) throws IOException {
        File sourceFile = new File(testPkgDir, classFileName);
        FileUtils.writeStringToFile(sourceFile, sourceCode, Charset.defaultCharset());

        // Setup and call a JavaCompiler with our annotation processor
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile));
        JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null, null, null, compilationUnits);
        Processor processor = new TestGenStaticModelProcessor(options);
        task.setProcessors(List.of(processor));
        task.call();
    }

    /**
     * This class allows us to override the processingEnvironment options.
     * Otherwise, they are deeply hidden behind the javac internal classes
     *
     * Need to repeat annotations here, because they don't get inherited.
     */
    @SupportedAnnotationTypes("moonlight.annotationproc.staticmeta.GenStaticModel")
    @SupportedSourceVersion(SourceVersion.RELEASE_17)
    public static class TestGenStaticModelProcessor extends  GenStaticModelProcessor {
        private final Map<String, String> customOptions;
        public TestGenStaticModelProcessor(Map<String, String> opts) {
            this.customOptions = Optional.ofNullable(opts).orElse(Collections.emptyMap());
        }
        @Override
        protected String getOption(String key) {
            return customOptions.get(key);
        }
    }
}