package io.quarkus.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;

/**
 * Builds the GraalVM Native Image Bundle as part of a dry run build.
 */
@Mojo(name = "create-native-image-bundle", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, threadSafe = true)
public class CreateNativeImageBundleMojo extends BuildMojo {

    private static final String QUARKUS_NATIVE_DRY_RUN_KEY = "quarkus.native.dry-run";

    @Override
    protected void doExecute() throws MojoExecutionException {
        if (isNativeProfileEnabled(mavenProject())) {
            setDryRun(true);

            try (CuratedApplication curatedApplication = bootstrapApplication()) {
                AugmentAction action = curatedApplication.createAugmentor();
                action.createProductionApplication();
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to build quarkus application in dry run mode", e);
            } finally {
                setDryRun(false);
            }
        }
    }

    private void setDryRun(boolean dryRun) {
        System.setProperty(QUARKUS_NATIVE_DRY_RUN_KEY, String.valueOf(dryRun));
    }

}
