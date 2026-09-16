package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaCapturingTypeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class NativeImageSerializationConfigStepTest {

    @Test
    void outputDoesNotDependOnTheOrderTheBuildItemsAreProducedIn() {
        // Build steps run concurrently, so the injected lists arrive in an arbitrary order. Reversing every list
        // stands in for that: the generated serialization-config.json must come out identical either way.
        List<ReflectiveClassBuildItem> classes = List.of(
                ReflectiveClassBuildItem.builder("org.acme.Zed").serialization().build(),
                ReflectiveClassBuildItem.builder("org.acme.Alpha").serialization().build());
        List<LambdaCapturingTypeBuildItem> lambdaCapturingTypes = List.of(
                new LambdaCapturingTypeBuildItem("org.acme.ZedFunction"),
                new LambdaCapturingTypeBuildItem("org.acme.AlphaFunction"));

        String json = generateSerializationConfigJson(classes, lambdaCapturingTypes);

        assertThat(json).isEqualTo(generateSerializationConfigJson(reversed(classes), reversed(lambdaCapturingTypes)));
        assertThat(json.indexOf("org.acme.Alpha\"")).isLessThan(json.indexOf("org.acme.Zed\""));
        assertThat(json.indexOf("AlphaFunction")).isLessThan(json.indexOf("ZedFunction"));
    }

    private static <T> List<T> reversed(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.reverse(copy);
        return copy;
    }

    private static String generateSerializationConfigJson(List<ReflectiveClassBuildItem> reflectiveClassBuildItems,
            List<LambdaCapturingTypeBuildItem> lambdaCapturingTypeBuildItems) {
        NativeImageSerializationConfigStep step = new NativeImageSerializationConfigStep();
        List<GeneratedResourceBuildItem> produced = new ArrayList<>();

        step.generateSerializationConfig(produced::add, reflectiveClassBuildItems, lambdaCapturingTypeBuildItems);

        assertThat(produced).hasSize(1);
        assertThat(produced.get(0).getName()).isEqualTo("META-INF/native-image/serialization-config.json");
        return new String(produced.get(0).getData(), StandardCharsets.UTF_8).replaceAll("\\s+", "");
    }
}
