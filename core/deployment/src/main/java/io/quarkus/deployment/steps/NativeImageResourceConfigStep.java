package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageResourceConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateResourceConfig(BuildProducer<GeneratedResourceBuildItem> resourceConfig,
            List<NativeImageResourcePatternsBuildItem> resourcePatterns,
            List<NativeImageResourceBundleBuildItem> resourceBundles,
            List<NativeImageResourceBuildItem> resources,
            List<ServiceProviderBuildItem> serviceProviderBuildItems) {
        JsonObjectBuilder root = Json.object();

        // Build steps run concurrently, so the injected lists arrive in a different order on every build. Sorting
        // the patterns and bundle names keeps resource-config.json byte-identical across builds of identical
        // sources; native-image applies them as a set, so the order carries no meaning.
        List<String> includePatterns = new ArrayList<>();

        for (NativeImageResourceBuildItem i : resources) {
            for (String path : i.getResources()) {
                includePatterns.add(Pattern.quote(path));
            }
        }

        for (ServiceProviderBuildItem i : serviceProviderBuildItems) {
            includePatterns.add(Pattern.quote(i.serviceDescriptorFile()));
        }

        for (NativeImageResourcePatternsBuildItem resourcePatternsItem : resourcePatterns) {
            includePatterns.addAll(resourcePatternsItem.getIncludePatterns());
        }
        Collections.sort(includePatterns);

        JsonObjectBuilder resourcesJs = Json.object();
        JsonArrayBuilder includes = Json.array();
        for (String includePattern : includePatterns) {
            includes.add(Json.object().put("pattern", includePattern));
        }
        resourcesJs.put("includes", includes);
        root.put("resources", resourcesJs);

        List<String> bundleNames = new ArrayList<>();
        for (NativeImageResourceBundleBuildItem i : resourceBundles) {
            String moduleName = i.getModuleName();
            StringBuilder sb = new StringBuilder();
            if (moduleName != null) {
                sb.append(moduleName).append(":");
            }
            sb.append(i.getBundleName().replace("/", "."));
            bundleNames.add(sb.toString());
        }
        Collections.sort(bundleNames);

        JsonArrayBuilder bundles = Json.array();
        for (String bundleName : bundleNames) {
            bundles.add(Json.object().put("name", bundleName));
        }
        root.put("bundles", bundles);

        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            resourceConfig.produce(new GeneratedResourceBuildItem("META-INF/native-image/resource-config.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
