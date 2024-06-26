package io.quarkus.test.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import io.quarkus.test.common.WithTestResource.List;

/**
 * Used to define a test resource.
 *
 * <b>All</b> {@code WithTestResource} annotations in the test module
 * are discovered (regardless of the test which contains the annotation)
 * and their corresponding {@code QuarkusTestResourceLifecycleManager}
 * started <b>before</b> <b>any</b> test is run.
 *
 * Note that test resources are never restarted when running {@code @Nested} test classes.
 * <p>
 * This replaces {@link QuarkusTestResource}. The only difference is that the default value for
 * {@link #restrictToAnnotatedClass()} {@code == true}.
 * </p>
 * <p>
 * This means that any resources managed by {@link #value()} apply to an individual test class or test profile, unlike with
 * {@link QuarkusTestResource} where a resource applies to all test classes.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(List.class)
public @interface WithTestResource {

    /**
     * @return The class managing the lifecycle of the test resource.
     */
    Class<? extends QuarkusTestResourceLifecycleManager> value();

    /**
     * @return The arguments to be passed to the {@code QuarkusTestResourceLifecycleManager}
     *
     * @see QuarkusTestResourceLifecycleManager#init(Map)
     */
    ResourceArg[] initArgs() default {};

    /**
     * Whether this test resource is to be started in parallel (concurrently) along with others also marked as parallel
     */
    boolean parallel() default false;

    /**
     * Whether this annotation should only be enabled if it is placed on the currently running test class or test profile.
     * Note that this defaults to true for meta-annotations since meta-annotations are only considered
     * for the current test class or test profile.
     */
    boolean restrictToAnnotatedClass() default true;

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        WithTestResource[] value();
    }
}
