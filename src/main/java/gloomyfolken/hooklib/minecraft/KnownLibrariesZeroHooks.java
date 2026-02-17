package gloomyfolken.hooklib.minecraft;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class KnownLibrariesZeroHooks {
    public static final String jrePrefix = System.getProperty("java.home");
    public static final Set<String> libsNames = ImmutableSet.of(
        "rt.jar",
        "jsse.jar",
        "java-objc-bridge-1.0.0.jar",
        "jsr305-3.0.1.jar",
        "gson-2.8.0.jar",
        "guava-21.0.jar",
        "realms-1.10.22.jar",
        "icu4j-core-mojang-51.2.jar",
        "authlib-1.5.25.jar",
        "patchy-1.3.9.jar",
        "text2speech-1.10.3.jar",
        "text2speech-1.10.3-natives-windows.jar",
        "akka-actor_2.11-2.3.3.jar",
        "config-1.2.1.jar",
        "commons-codec-1.10.jar",
        "commons-io-2.5.jar",
        "commons-logging-1.1.3.jar",
        "netty-all-4.1.9.Final.jar",
        "fastutil-7.1.0.jar",
        "vecmath-1.5.2.jar",
        "lzma-0.0.1.jar",
        "jna-4.4.0.jar",
        "platform-3.4.0.jar",
        "launchwrapper-1.12.jar",
        "jopt-simple-5.0.4.jar",
        "trove4j-3.0.3.jar",
        "commons-compress-1.8.1.jar",
        "commons-lang3-3.5.jar",
        "httpclient-4.3.3.jar",
        "httpcore-4.3.2.jar",
        "log4j-api-2.15.0.jar",
        "log4j-core-2.15.0.jar",
        "plexus-utils-3.1.0.jar",
        "jline-3.5.1.jar",
        "asm-analysis-6.2.jar",
        "asm-debug-all-5.2.jar",
        "asm-tree-6.2.jar",
        "asm-util-6.2.jar",
        "asm-6.2.jar",
        "scala-parser-combinators_2.11-1.0.1.jar",
        "scala-xml_2.11-1.0.2.jar",
        "scala-continuations-library_2.11-1.0.2_mc.jar",
        "scala-continuations-plugin_2.11.1-1.0.2_mc.jar",
        "scala-actors-migration_2.11-1.1.0.jar",
        "scala-actors-2.11.0.jar",
        "scala-compiler-2.11.1.jar",
        "scala-library-2.11.1.jar",
        "scala-reflect-2.11.1.jar",
        "scala-swing_2.11-1.0.1.jar",
        "oshi-core-1.1.jar",
        "forge-1.12.2-14.23.5.2860_mapped_stable_39-1.12.jar",
        "!mixinbooter-10.6.jar"
    );
}
