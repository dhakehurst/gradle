/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.classpath

import org.gradle.internal.classpath.ClassPath
import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.internal.installation.GradleInstallation
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

class DefaultModuleRegistryTest extends Specification {
    @Rule
    final TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()
    TestFile runtimeDep
    TestFile resourcesDir
    TestFile jarFile
    TestFile distDir

    def setup() {
        distDir = tmpDir.createDir("dist")

        distDir.createDir("lib")
        distDir.createDir("lib/plugins")
        distDir.createDir("lib/plugins/sonar")
        runtimeDep = distDir.createZip("lib/dep-1.2.jar")

        resourcesDir = tmpDir.createDir("some-module/build/resources/main")
        save(
            properties(runtime: 'dep-1.2.jar', projects: ''),
            resourcesDir.file("gradle-some-module-classpath.properties"))

        jarFile = distDir.file("lib/gradle-some-module-5.1.jar")
        resourcesDir.zipTo(jarFile)
    }

    def "locates module using jar in distribution image"() {
        given:
        def registry = new DefaultModuleRegistry(classLoaderFor([]), ClassPath.EMPTY, new GradleInstallation(distDir))

        expect:
        def module = registry.getModule("gradle-some-module")
        module.implementationClasspath.asFiles == [jarFile]
        module.runtimeClasspath.asFiles == [runtimeDep]
    }

    def "locates module using jar from runtime ClassLoader"() {
        given:
        def cl = classLoaderFor([jarFile, runtimeDep])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, null)

        expect:
        def module = registry.getModule("gradle-some-module")
        module.implementationClasspath.asFiles == [jarFile]
        module.runtimeClasspath.asFiles == [runtimeDep]
    }

    def "locates module using manifest from runtime ClassLoader when run from IDEA"() {
        given:
        def classesDir = tmpDir.createDir("out/production/someModule")
        def staticResourcesDir = tmpDir.createDir("some-module/src/main/resources")
        def ignoredDir = tmpDir.createDir("ignore-me-out/production/someModule")
        def cl = classLoaderFor([ignoredDir, classesDir, resourcesDir, staticResourcesDir, runtimeDep])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, null)

        expect:
        def module = registry.getModule("gradle-some-module")
        module.implementationClasspath.asFiles == [classesDir, resourcesDir, staticResourcesDir]
        module.runtimeClasspath.asFiles == [runtimeDep]
    }

    def "locates module using manifest from runtime ClassLoader when run from Eclipse"() {
        given:
        def classesDir = tmpDir.createDir("some-module/bin")
        def staticResourcesDir = tmpDir.createDir("some-module/src/main/resources")
        def cl = classLoaderFor([classesDir, resourcesDir, staticResourcesDir, runtimeDep])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, null)

        expect:
        def module = registry.getModule("gradle-some-module")
        module.implementationClasspath.asFiles == [classesDir, resourcesDir, staticResourcesDir]
        module.runtimeClasspath.asFiles == [runtimeDep]
    }

    def "locates module using manifest from runtime ClassLoader when run from build"() {
        given:
        def classesDir = tmpDir.createDir("some-module/build/classes/main")
        def staticResourcesDir = tmpDir.createDir("some-module/src/main/resources")
        def cl = classLoaderFor([classesDir, resourcesDir, staticResourcesDir, runtimeDep])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, null)

        expect:
        def module = registry.getModule("gradle-some-module")
        module.implementationClasspath.asFiles == [classesDir, resourcesDir, staticResourcesDir]
        module.runtimeClasspath.asFiles == [runtimeDep]
    }

    def "locates module using jar from additional classpath"() {
        given:
        def registry = new DefaultModuleRegistry(new DefaultClassPath([jarFile, runtimeDep]), null)

        expect:
        def module = registry.getModule("gradle-some-module")
        module.implementationClasspath.asFiles == [jarFile]
        module.runtimeClasspath.asFiles == [runtimeDep]
    }

    def "locates module using manifest from additional classpath when run from IDEA"() {
        given:
        def classesDir = tmpDir.createDir("out/production/someModule")
        def staticResourcesDir = tmpDir.createDir("some-module/src/main/resources")
        def ignoredDir = tmpDir.createDir("ignore-me-out/production/someModule")
        def registry = new DefaultModuleRegistry(new DefaultClassPath([ignoredDir, classesDir, resourcesDir, staticResourcesDir, runtimeDep]), null)

        expect:
        def module = registry.getModule("gradle-some-module")
        module.implementationClasspath.asFiles == [classesDir, resourcesDir, staticResourcesDir]
        module.runtimeClasspath.asFiles == [runtimeDep]
    }

    def "requires no additional module classpath when run from distribution"() {
        given:
        def registry = new DefaultModuleRegistry(classLoaderFor([]), ClassPath.EMPTY, new GradleInstallation(distDir))

        expect:
        registry.additionalClassPath.empty
    }

    def "requires additional module classpath when no distribution available"() {
        given:
        def classesDir = tmpDir.createDir("out/production/someModule")
        def staticResourcesDir = tmpDir.createDir("some-module/src/main/resources")
        def ignoredDir = tmpDir.createDir("ignore-me-out/production/someModule")
        def cl = classLoaderFor([ignoredDir, classesDir, resourcesDir, staticResourcesDir, runtimeDep])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, null)

        expect:
        registry.additionalClassPath.asFiles.containsAll([classesDir, staticResourcesDir, resourcesDir])
    }

    def "handles empty classpaths in manifest"() {
        given:
        save(
            properties(runtime: '', projects: ''),
            resourcesDir.file("gradle-some-module-classpath.properties"))

        def cl = classLoaderFor([resourcesDir, runtimeDep])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, null)

        expect:
        def module = registry.getModule("gradle-some-module")
        module.runtimeClasspath.empty
        module.requiredModules.empty
    }

    def "extracts required modules from manifest"() {
        given:
        def module1Dir = tmpDir.createDir("some-module/build/resources/main")
        save(
            properties(projects: 'gradle-module-2'),
            module1Dir.file("gradle-some-module-classpath.properties"))

        def module2Dir = tmpDir.createDir("module-2/build/resources/main")
        save(properties(), module2Dir.file("gradle-module-2-classpath.properties"))

        def cl = classLoaderFor([module1Dir, module2Dir])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, null)

        expect:
        def module = registry.getModule("gradle-some-module")
        module.requiredModules as List == [registry.getModule("gradle-module-2")]
    }

    def "builds transitive closure of required modules"() {
        given:
        def module1Dir = tmpDir.createDir("some-module/build/resources/main")
        save(
            properties(projects: 'gradle-module-2'),
            module1Dir.file("gradle-some-module-classpath.properties"))

        def module2Dir = tmpDir.createDir("module-2/build/resources/main")
        save(
            properties(projects: 'gradle-module-3'),
            module2Dir.file("gradle-module-2-classpath.properties"))

        def module3Dir = tmpDir.createDir("module-3/build/resources/main")
        save(
            properties(projects: ''),
            module3Dir.file("gradle-module-3-classpath.properties"))

        def cl = classLoaderFor([module1Dir, module2Dir, module3Dir])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, null)

        expect:
        def module = registry.getModule("gradle-some-module")
        module.allRequiredModules as List == [module, registry.getModule("gradle-module-2"), registry.getModule("gradle-module-3")]
    }

    def "supports cycles between modules"() {
        given:
        def module1Dir = tmpDir.createDir("module-1/build/resources/main")
        save(
            properties(projects: 'gradle-module-2'),
            module1Dir.file("gradle-module-1-classpath.properties"))

        def module2Dir = tmpDir.createDir("module-2/build/resources/main")
        save(
            properties(projects: 'gradle-module-1'),
            module2Dir.file("gradle-module-2-classpath.properties"))

        def cl = classLoaderFor([module1Dir, module2Dir])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, null)

        expect:
        def module = registry.getModule("gradle-module-1")
        module.allRequiredModules as List == [module, registry.getModule("gradle-module-2")]
    }

    def "fails when classpath does not contain manifest resource"() {
        given:
        def registry = new DefaultModuleRegistry(classLoaderFor([]), ClassPath.EMPTY, null)

        when:
        registry.getModule("gradle-some-module")

        then:
        UnknownModuleException e = thrown()
        e.message == "Cannot locate manifest for module 'gradle-some-module' in classpath."
    }

    def "fails when classpath and distribution image do not contain manifest"() {
        given:
        def registry = new DefaultModuleRegistry(classLoaderFor([]), ClassPath.EMPTY, new GradleInstallation(distDir))

        when:
        registry.getModule("gradle-other-module")

        then:
        UnknownModuleException e = thrown()
        e.message == "Cannot locate JAR for module 'gradle-other-module' in distribution directory '$distDir'."
    }

    def "locates an external module as a JAR on the classpath"() {
        given:
        def cl = classLoaderFor([runtimeDep])
        def registry = new DefaultModuleRegistry(cl, ClassPath.EMPTY, new GradleInstallation(distDir))

        expect:
        def module = registry.getExternalModule("dep")
        module.implementationClasspath.asFiles == [runtimeDep]
        module.runtimeClasspath.empty
    }

    def "locates an external module as a JAR in the distribution image when not available on the classpath"() {
        given:
        def registry = new DefaultModuleRegistry(classLoaderFor([]), ClassPath.EMPTY, new GradleInstallation(distDir))

        expect:
        def module = registry.getExternalModule("dep")
        module.implementationClasspath.asFiles == [runtimeDep]
        module.runtimeClasspath.empty
    }

    def "fails when external module cannot be found"() {
        given:
        def registry = new DefaultModuleRegistry(classLoaderFor([]), ClassPath.EMPTY, new GradleInstallation(distDir))

        when:
        registry.getExternalModule("unknown")

        then:
        UnknownModuleException e = thrown()
        e.message == "Cannot locate JAR for module 'unknown' in distribution directory '$distDir'."
    }

    def "ignores jars which have the same prefix as an external module"() {
        given:
        distDir.createFile("dep-launcher-1.2.jar")
        distDir.createFile("dep-launcher-1.2-beta-3.jar")
        def registry = new DefaultModuleRegistry(classLoaderFor([]), ClassPath.EMPTY, new GradleInstallation(distDir))

        expect:
        def module = registry.getExternalModule("dep")
        module.implementationClasspath.asFiles == [runtimeDep]
        module.runtimeClasspath.empty
    }

    def "also looks in subdirectories of plugins directory when searching for external modules"() {
        given:
        def registry = new DefaultModuleRegistry(classLoaderFor([]), ClassPath.EMPTY, new GradleInstallation(distDir))

        when:
        def sonarDependency = distDir.createZip("lib/plugins/sonar/sonar-dependency-1.2.jar")

        then:
        def module = registry.getExternalModule("sonar-dependency")
        module.implementationClasspath.asFiles == [sonarDependency]
        module.runtimeClasspath.empty
    }

    private Properties properties(Map kvs = [:]) {
        new Properties().with {
            putAll(kvs)
            it
        }
    }

    private URLClassLoader classLoaderFor(Iterable<TestFile> files) {
        new URLClassLoader(files.collect { it.toURI().toURL() } as URL[])
    }

    private def save(Properties properties, TestFile file) {
        file.withOutputStream { outstr -> properties.save(outstr, "header") }
    }
}
