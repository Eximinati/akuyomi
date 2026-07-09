package org.gradle.accessors.dm;

import org.gradle.api.NonNullApi;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.plugin.use.PluginDependency;
import org.gradle.api.artifacts.ExternalModuleDependencyBundle;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.provider.Provider;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.internal.catalog.AbstractExternalDependencyFactory;
import org.gradle.api.internal.catalog.DefaultVersionCatalog;
import java.util.Map;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.api.internal.artifacts.dsl.CapabilityNotationParser;
import javax.inject.Inject;

/**
 * A catalog of dependencies accessible via the `libs` extension.
 */
@NonNullApi
public class LibrariesForLibs extends AbstractExternalDependencyFactory {

    private final AbstractExternalDependencyFactory owner = this;
    private final AniyomiLibraryAccessors laccForAniyomiLibraryAccessors = new AniyomiLibraryAccessors(owner);
    private final CoroutinesLibraryAccessors laccForCoroutinesLibraryAccessors = new CoroutinesLibraryAccessors(owner);
    private final GradleLibraryAccessors laccForGradleLibraryAccessors = new GradleLibraryAccessors(owner);
    private final InjektLibraryAccessors laccForInjektLibraryAccessors = new InjektLibraryAccessors(owner);
    private final KotlinLibraryAccessors laccForKotlinLibraryAccessors = new KotlinLibraryAccessors(owner);
    private final VersionAccessors vaccForVersionAccessors = new VersionAccessors(providers, config);
    private final BundleAccessors baccForBundleAccessors = new BundleAccessors(objects, providers, config, attributesFactory, capabilityNotationParser);
    private final PluginAccessors paccForPluginAccessors = new PluginAccessors(providers, config);

    @Inject
    public LibrariesForLibs(DefaultVersionCatalog config, ProviderFactory providers, ObjectFactory objects, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) {
        super(config, providers, objects, attributesFactory, capabilityNotationParser);
    }

        /**
         * Creates a dependency provider for jsoup (org.jsoup:jsoup)
     * with version '1.16.1'.
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getJsoup() {
            return create("jsoup");
    }

        /**
         * Creates a dependency provider for okhttp (com.squareup.okhttp3:okhttp)
     * with version '5.0.0-alpha.11'.
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getOkhttp() {
            return create("okhttp");
    }

        /**
         * Creates a dependency provider for quickjs (app.cash.quickjs:quickjs-android)
     * with version '0.9.2'.
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getQuickjs() {
            return create("quickjs");
    }

        /**
         * Creates a dependency provider for rxjava (io.reactivex:rxjava)
     * with version '1.3.8'.
         * This dependency was declared in catalog libs.versions.toml
         */
        public Provider<MinimalExternalModuleDependency> getRxjava() {
            return create("rxjava");
    }

    /**
     * Returns the group of libraries at aniyomi
     */
    public AniyomiLibraryAccessors getAniyomi() {
        return laccForAniyomiLibraryAccessors;
    }

    /**
     * Returns the group of libraries at coroutines
     */
    public CoroutinesLibraryAccessors getCoroutines() {
        return laccForCoroutinesLibraryAccessors;
    }

    /**
     * Returns the group of libraries at gradle
     */
    public GradleLibraryAccessors getGradle() {
        return laccForGradleLibraryAccessors;
    }

    /**
     * Returns the group of libraries at injekt
     */
    public InjektLibraryAccessors getInjekt() {
        return laccForInjektLibraryAccessors;
    }

    /**
     * Returns the group of libraries at kotlin
     */
    public KotlinLibraryAccessors getKotlin() {
        return laccForKotlinLibraryAccessors;
    }

    /**
     * Returns the group of versions at versions
     */
    public VersionAccessors getVersions() {
        return vaccForVersionAccessors;
    }

    /**
     * Returns the group of bundles at bundles
     */
    public BundleAccessors getBundles() {
        return baccForBundleAccessors;
    }

    /**
     * Returns the group of plugins at plugins
     */
    public PluginAccessors getPlugins() {
        return paccForPluginAccessors;
    }

    public static class AniyomiLibraryAccessors extends SubDependencyFactory {

        public AniyomiLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for lib (com.github.aniyomiorg:extensions-lib)
         * with version '14'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getLib() {
                return create("aniyomi.lib");
        }

    }

    public static class CoroutinesLibraryAccessors extends SubDependencyFactory {

        public CoroutinesLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for android (org.jetbrains.kotlinx:kotlinx-coroutines-android)
         * with versionRef 'coroutines.version'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getAndroid() {
                return create("coroutines.android");
        }

            /**
             * Creates a dependency provider for core (org.jetbrains.kotlinx:kotlinx-coroutines-core)
         * with versionRef 'coroutines.version'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getCore() {
                return create("coroutines.core");
        }

    }

    public static class GradleLibraryAccessors extends SubDependencyFactory {
        private final GradleKotlinLibraryAccessors laccForGradleKotlinLibraryAccessors = new GradleKotlinLibraryAccessors(owner);

        public GradleLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for agp (com.android.tools.build:gradle)
         * with versionRef 'agp.version'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getAgp() {
                return create("gradle.agp");
        }

            /**
             * Creates a dependency provider for kotlinter (org.jmailen.gradle:kotlinter-gradle)
         * with version '3.15.0'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getKotlinter() {
                return create("gradle.kotlinter");
        }

        /**
         * Returns the group of libraries at gradle.kotlin
         */
        public GradleKotlinLibraryAccessors getKotlin() {
            return laccForGradleKotlinLibraryAccessors;
        }

    }

    public static class GradleKotlinLibraryAccessors extends SubDependencyFactory implements DependencyNotationSupplier {

        public GradleKotlinLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for kotlin (org.jetbrains.kotlin:kotlin-gradle-plugin)
         * with versionRef 'kotlin.version'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> asProvider() {
                return create("gradle.kotlin");
        }

            /**
             * Creates a dependency provider for serialization (org.jetbrains.kotlin:kotlin-serialization)
         * with versionRef 'kotlin.version'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getSerialization() {
                return create("gradle.kotlin.serialization");
        }

    }

    public static class InjektLibraryAccessors extends SubDependencyFactory {

        public InjektLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for core (com.github.null2264.injekt:injekt-core)
         * with version '4135455a2a'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getCore() {
                return create("injekt.core");
        }

    }

    public static class KotlinLibraryAccessors extends SubDependencyFactory {

        public KotlinLibraryAccessors(AbstractExternalDependencyFactory owner) { super(owner); }

            /**
             * Creates a dependency provider for json (org.jetbrains.kotlinx:kotlinx-serialization-json)
         * with versionRef 'serialization.version'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getJson() {
                return create("kotlin.json");
        }

            /**
             * Creates a dependency provider for protobuf (org.jetbrains.kotlinx:kotlinx-serialization-protobuf)
         * with versionRef 'serialization.version'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getProtobuf() {
                return create("kotlin.protobuf");
        }

            /**
             * Creates a dependency provider for stdlib (org.jetbrains.kotlin:kotlin-stdlib-jdk8)
         * with versionRef 'kotlin.version'.
             * This dependency was declared in catalog libs.versions.toml
             */
            public Provider<MinimalExternalModuleDependency> getStdlib() {
                return create("kotlin.stdlib");
        }

    }

    public static class VersionAccessors extends VersionFactory  {

        private final AgpVersionAccessors vaccForAgpVersionAccessors = new AgpVersionAccessors(providers, config);
        private final CoroutinesVersionAccessors vaccForCoroutinesVersionAccessors = new CoroutinesVersionAccessors(providers, config);
        private final KotlinVersionAccessors vaccForKotlinVersionAccessors = new KotlinVersionAccessors(providers, config);
        private final SerializationVersionAccessors vaccForSerializationVersionAccessors = new SerializationVersionAccessors(providers, config);
        public VersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

        /**
         * Returns the group of versions at versions.agp
         */
        public AgpVersionAccessors getAgp() {
            return vaccForAgpVersionAccessors;
        }

        /**
         * Returns the group of versions at versions.coroutines
         */
        public CoroutinesVersionAccessors getCoroutines() {
            return vaccForCoroutinesVersionAccessors;
        }

        /**
         * Returns the group of versions at versions.kotlin
         */
        public KotlinVersionAccessors getKotlin() {
            return vaccForKotlinVersionAccessors;
        }

        /**
         * Returns the group of versions at versions.serialization
         */
        public SerializationVersionAccessors getSerialization() {
            return vaccForSerializationVersionAccessors;
        }

    }

    public static class AgpVersionAccessors extends VersionFactory  {

        public AgpVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Returns the version associated to this alias: agp.version (8.2.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getVersion() { return getVersion("agp.version"); }

    }

    public static class CoroutinesVersionAccessors extends VersionFactory  {

        public CoroutinesVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Returns the version associated to this alias: coroutines.version (1.7.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getVersion() { return getVersion("coroutines.version"); }

    }

    public static class KotlinVersionAccessors extends VersionFactory  {

        public KotlinVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Returns the version associated to this alias: kotlin.version (1.8.22)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getVersion() { return getVersion("kotlin.version"); }

    }

    public static class SerializationVersionAccessors extends VersionFactory  {

        public SerializationVersionAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

            /**
             * Returns the version associated to this alias: serialization.version (1.5.1)
             * If the version is a rich version and that its not expressible as a
             * single version string, then an empty string is returned.
             * This version was declared in catalog libs.versions.toml
             */
            public Provider<String> getVersion() { return getVersion("serialization.version"); }

    }

    public static class BundleAccessors extends BundleFactory {

        public BundleAccessors(ObjectFactory objects, ProviderFactory providers, DefaultVersionCatalog config, ImmutableAttributesFactory attributesFactory, CapabilityNotationParser capabilityNotationParser) { super(objects, providers, config, attributesFactory, capabilityNotationParser); }

            /**
             * Creates a dependency bundle provider for common which is an aggregate for the following dependencies:
             * <ul>
             *    <li>org.jetbrains.kotlin:kotlin-stdlib-jdk8</li>
             *    <li>com.github.null2264.injekt:injekt-core</li>
             *    <li>io.reactivex:rxjava</li>
             *    <li>org.jetbrains.kotlinx:kotlinx-serialization-protobuf</li>
             *    <li>org.jetbrains.kotlinx:kotlinx-serialization-json</li>
             *    <li>org.jsoup:jsoup</li>
             *    <li>com.squareup.okhttp3:okhttp</li>
             *    <li>com.github.aniyomiorg:extensions-lib</li>
             *    <li>app.cash.quickjs:quickjs-android</li>
             *    <li>org.jetbrains.kotlinx:kotlinx-coroutines-core</li>
             *    <li>org.jetbrains.kotlinx:kotlinx-coroutines-android</li>
             * </ul>
             * This bundle was declared in catalog libs.versions.toml
             */
            public Provider<ExternalModuleDependencyBundle> getCommon() {
                return createBundle("common");
            }

    }

    public static class PluginAccessors extends PluginFactory {

        public PluginAccessors(ProviderFactory providers, DefaultVersionCatalog config) { super(providers, config); }

    }

}
