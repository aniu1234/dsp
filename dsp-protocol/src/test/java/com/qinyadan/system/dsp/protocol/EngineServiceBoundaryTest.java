package com.qinyadan.system.dsp.protocol;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;

/**
 * Prevent protocol handlers from bypassing the engine service facade.
 */
public class EngineServiceBoundaryTest {

    private static final List<String> FORBIDDEN_IMPORTS = Arrays.asList(
            "com.qinyadan.system.dsp.engine.calcite",
            "com.qinyadan.system.dsp.engine.data",
            "com.qinyadan.system.dsp.engine.meta",
            "com.qinyadan.system.dsp.engine.operator",
            "com.qinyadan.system.dsp.engine.rel");

    @Test
    public void handlersUseOnlyEngineServiceBoundary() throws IOException {
        Path protocolRoot = Paths.get(System.getProperty("basedir",
                        System.getProperty("user.dir")),
                "src/main/java/com/qinyadan/system/dsp/protocol");
        assertNoForbiddenImports(protocolRoot.resolve("command"));
        assertNoForbiddenImports(protocolRoot.resolve("visitor"));
    }

    private void assertNoForbiddenImports(Path sourceRoot) throws IOException {
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith(".java"))
                    .forEach(this::assertFileHasNoForbiddenImports);
        }
    }

    private void assertFileHasNoForbiddenImports(Path sourceFile) {
        final String source;
        try {
            source = new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot inspect " + sourceFile, e);
        }
        for (String forbiddenImport : FORBIDDEN_IMPORTS) {
            assertFalse(sourceFile + " bypasses the engine service boundary",
                    source.contains("import " + forbiddenImport));
        }
    }
}
