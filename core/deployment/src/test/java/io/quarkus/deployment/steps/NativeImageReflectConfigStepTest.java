package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.pkg.TestNativeConfig;

class NativeImageReflectConfigStepTest {

    private static final String CLASS_NAME = "org.acme.Foo";

    @Test
    void publicMethodsRegistersAllPublicMethodsNotAllDeclaredMethods() {
        String json = generateReflectConfigJson(List.of(
                ReflectiveClassBuildItem.builder(CLASS_NAME).constructors(false).publicMethods().build()));

        assertThat(json).contains("\"allPublicMethods\":true");
        assertThat(json).doesNotContain("allDeclaredMethods");
    }

    @Test
    void registeringSameClassWithMethodsAndPublicMethodsMergesBothFlags() {
        // Simulates two independent build steps registering the same class differently - the resulting
        // reflect-config.json entry must be the union of both, not just the last one applied.
        String json = generateReflectConfigJson(List.of(
                ReflectiveClassBuildItem.builder(CLASS_NAME).constructors(false).publicMethods().build(),
                ReflectiveClassBuildItem.builder(CLASS_NAME).constructors(false).methods().build()));

        assertThat(json).contains("\"allPublicMethods\":true");
        assertThat(json).contains("\"allDeclaredMethods\":true");
    }

    @Test
    void outputDoesNotDependOnTheOrderTheBuildItemsAreProducedIn() {
        // Build steps run concurrently, so the injected lists arrive in an arbitrary order. Reversing every list
        // stands in for that: the generated reflect-config.json must come out identical either way.
        List<ReflectiveClassBuildItem> classes = List.of(
                ReflectiveClassBuildItem.builder("org.acme.Zed").build(),
                ReflectiveClassBuildItem.builder("org.acme.Alpha").build());
        List<ReflectiveFieldBuildItem> fields = List.of(
                new ReflectiveFieldBuildItem(null, CLASS_NAME, "zedField"),
                new ReflectiveFieldBuildItem(null, CLASS_NAME, "alphaField"));
        List<ReflectiveMethodBuildItem> methods = List.of(
                new ReflectiveMethodBuildItem(CLASS_NAME, "zedMethod", new String[0]),
                new ReflectiveMethodBuildItem(CLASS_NAME, "alphaMethod", new String[0]),
                new ReflectiveMethodBuildItem(CLASS_NAME, "overloaded", new String[] { "java.lang.String" }),
                new ReflectiveMethodBuildItem(CLASS_NAME, "overloaded", new String[] { "java.lang.Integer" }));

        String json = generateReflectConfigJson(methods, fields, classes);

        assertThat(json).isEqualTo(generateReflectConfigJson(reversed(methods), reversed(fields), reversed(classes)));
        assertThat(json.indexOf("org.acme.Alpha")).isLessThan(json.indexOf("org.acme.Zed"));
        assertThat(json.indexOf("alphaField")).isLessThan(json.indexOf("zedField"));
        assertThat(json.indexOf("alphaMethod")).isLessThan(json.indexOf("zedMethod"));
        assertThat(json.indexOf("java.lang.Integer")).isLessThan(json.indexOf("java.lang.String"));
    }

    private static <T> List<T> reversed(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.reverse(copy);
        return copy;
    }

    private static String generateReflectConfigJson(List<ReflectiveClassBuildItem> reflectiveClassBuildItems) {
        return generateReflectConfigJson(List.of(), List.of(), reflectiveClassBuildItems);
    }

    private static String generateReflectConfigJson(List<ReflectiveMethodBuildItem> reflectiveMethods,
            List<ReflectiveFieldBuildItem> reflectiveFields,
            List<ReflectiveClassBuildItem> reflectiveClassBuildItems) {
        NativeImageReflectConfigStep step = new NativeImageReflectConfigStep();
        List<GeneratedResourceBuildItem> produced = new ArrayList<>();

        step.generateReflectConfig(produced::add, new TestNativeConfig("mandrel"),
                reflectiveMethods, reflectiveFields,
                reflectiveClassBuildItems,
                List.of(), List.of(), List.of());

        assertThat(produced).hasSize(1);
        assertThat(produced.get(0).getName()).isEqualTo("META-INF/native-image/reflect-config.json");
        return new String(produced.get(0).getData(), StandardCharsets.UTF_8).replaceAll("\\s+", "");
    }
}
