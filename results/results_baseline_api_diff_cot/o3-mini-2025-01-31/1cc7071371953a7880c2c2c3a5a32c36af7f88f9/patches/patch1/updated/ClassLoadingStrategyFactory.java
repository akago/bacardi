package org.assertj.vavr.api;

import io.vavr.control.Try;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

class ClassLoadingStrategyFactory {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final Method PRIVATE_LOOKUP_IN = Try.of(
        () -> MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class)
    ).getOrElse((Method) null);

    static ClassLoadingStrategy<ClassLoader> classLoadingStrategy(Class<?> assertClass) {
        // The previous dynamic strategies using ClassInjector have been removed from the dependency.
        // We now always return ByteBuddy’s default injection strategy.
        return ClassLoadingStrategy.Default.INJECTION;
    }

}