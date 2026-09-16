package io.quarkus.deployment.steps;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.bootstrap.json.Json;
import io.quarkus.bootstrap.json.Json.JsonArrayBuilder;
import io.quarkus.bootstrap.json.Json.JsonObjectBuilder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.LambdaCapturingTypeBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

public class NativeImageSerializationConfigStep {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void generateSerializationConfig(BuildProducer<GeneratedResourceBuildItem> serializationConfig,
            List<ReflectiveClassBuildItem> reflectiveClassBuildItems,
            List<LambdaCapturingTypeBuildItem> lambdaCapturingTypeBuildItems) {

        // Build steps run concurrently, so the injected lists arrive in a different order on every build. Sorting
        // keeps serialization-config.json byte-identical across builds of identical sources; native-image treats
        // these entries as a set, so the order carries no meaning.
        final Set<String> serializableClasses = new TreeSet<>();
        for (ReflectiveClassBuildItem i : reflectiveClassBuildItems) {
            if (i.isSerialization()) {
                String[] classNames = i.getClassNames().toArray(new String[0]);
                Collections.addAll(serializableClasses, classNames);
            }
        }

        JsonObjectBuilder root = Json.object();
        JsonArrayBuilder types = Json.array();
        for (String serializableClass : serializableClasses) {
            types.add(Json.object().put("name", serializableClass));
        }
        root.put("types", types);

        List<String> lambdaCapturingTypeNames = new ArrayList<>();
        for (LambdaCapturingTypeBuildItem i : lambdaCapturingTypeBuildItems) {
            lambdaCapturingTypeNames.add(i.getClassName());
        }
        Collections.sort(lambdaCapturingTypeNames);

        JsonArrayBuilder lambdaCapturingTypes = Json.array();
        for (String lambdaCapturingTypeName : lambdaCapturingTypeNames) {
            lambdaCapturingTypes.add(Json.object().put("name", lambdaCapturingTypeName));
        }
        root.put("lambdaCapturingTypes", lambdaCapturingTypes);

        try (StringWriter writer = new StringWriter()) {
            root.appendTo(writer);
            serializationConfig.produce(new GeneratedResourceBuildItem("META-INF/native-image/serialization-config.json",
                    writer.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
