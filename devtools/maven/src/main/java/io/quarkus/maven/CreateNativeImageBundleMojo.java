package io.quarkus.maven;

import java.util.*;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;

/**
 * Builds the Quarkus application.
 */
@Mojo(name = "create-native-image-bundle", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class CreateNativeImageBundleMojo extends BuildMojo {

    public static final String QUARKUS_NATIVE_ADDITIONAL_BUILD_ARGS = "quarkus.native.additional-build-args";
    public static final String BUILD_ARG_CREATE_NATIVE_IMAGE_BUNDLE_WITH_DRY_RUN = "--bundle-create=bundle.nib\\,dry-run";

    @Override
    protected void doExecute() throws MojoExecutionException {
        if (isNativeEnabled()) {
            String originalAdditionalBuildArgs = System.getProperty(QUARKUS_NATIVE_ADDITIONAL_BUILD_ARGS);
            try {
                addBundleCreateBuildArg(originalAdditionalBuildArgs);

                try (CuratedApplication curatedApplication = bootstrapApplication()) {
                    AugmentAction action = curatedApplication.createAugmentor();
                    action.createProductionApplication();
                }
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to build quarkus application", e);
            } finally {
                System.setProperty(QUARKUS_NATIVE_ADDITIONAL_BUILD_ARGS,
                        originalAdditionalBuildArgs == null ? "" : originalAdditionalBuildArgs);
            }
        }
    }

    private void addBundleCreateBuildArg(String originalAdditionalBuildArgs) {
        String additionalBuildArgs = BUILD_ARG_CREATE_NATIVE_IMAGE_BUNDLE_WITH_DRY_RUN;
        if (null != originalAdditionalBuildArgs) {
            additionalBuildArgs += " " + originalAdditionalBuildArgs;
        }
        System.setProperty(QUARKUS_NATIVE_ADDITIONAL_BUILD_ARGS, additionalBuildArgs);
    }

    private boolean isNativeEnabled() {
        return !System.getProperties().containsKey("quarkus.native.enabled") && isNativeProfileEnabled(mavenProject());
    }

}
