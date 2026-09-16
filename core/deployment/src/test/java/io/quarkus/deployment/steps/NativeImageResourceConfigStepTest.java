package io.quarkus.deployment.steps;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

class NativeImageResourceConfigStepTest {

    @Test
    void outputDoesNotDependOnTheOrderTheBuildItemsAreProducedIn() {
        // Build steps run concurrently, so the injected lists arrive in an arbitrary order. Reversing every list
        // stands in for that: the generated resource-config.json must come out identical either way.
        List<NativeImageResourcePatternsBuildItem> patterns = List.of(
                NativeImageResourcePatternsBuildItem.builder().includeGlob("zed/*.txt").build(),
                NativeImageResourcePatternsBuildItem.builder().includeGlob("alpha/*.txt").build());
        List<NativeImageResourceBundleBuildItem> bundles = List.of(
                new NativeImageResourceBundleBuildItem("org.acme.ZedBundle"),
                new NativeImageResourceBundleBuildItem("org.acme.AlphaBundle"));
        List<NativeImageResourceBuildItem> resources = List.of(
                new NativeImageResourceBuildItem("zed.properties"),
                new NativeImageResourceBuildItem("alpha.properties"));
        List<ServiceProviderBuildItem> serviceProviders = List.of(
                new ServiceProviderBuildItem("org.acme.ZedService", "org.acme.ZedServiceImpl"),
                new ServiceProviderBuildItem("org.acme.AlphaService", "org.acme.AlphaServiceImpl"));

        String json = generateResourceConfigJson(patterns, bundles, resources, serviceProviders);

        assertThat(json).isEqualTo(generateResourceConfigJson(reversed(patterns), reversed(bundles),
                reversed(resources), reversed(serviceProviders)));
        assertThat(json.indexOf("alpha.properties")).isLessThan(json.indexOf("zed.properties"));
        assertThat(json.indexOf("alpha/")).isLessThan(json.indexOf("zed/"));
        assertThat(json.indexOf("AlphaService")).isLessThan(json.indexOf("ZedService"));
        assertThat(json.indexOf("AlphaBundle")).isLessThan(json.indexOf("ZedBundle"));
    }

    private static <T> List<T> reversed(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        Collections.reverse(copy);
        return copy;
    }

    private static String generateResourceConfigJson(List<NativeImageResourcePatternsBuildItem> resourcePatterns,
            List<NativeImageResourceBundleBuildItem> resourceBundles,
            List<NativeImageResourceBuildItem> resources,
            List<ServiceProviderBuildItem> serviceProviderBuildItems) {
        NativeImageResourceConfigStep step = new NativeImageResourceConfigStep();
        List<GeneratedResourceBuildItem> produced = new ArrayList<>();

        step.generateResourceConfig(produced::add, resourcePatterns, resourceBundles, resources,
                serviceProviderBuildItems);

        assertThat(produced).hasSize(1);
        assertThat(produced.get(0).getName()).isEqualTo("META-INF/native-image/resource-config.json");
        return new String(produced.get(0).getData(), StandardCharsets.UTF_8).replaceAll("\\s+", "");
    }
}
