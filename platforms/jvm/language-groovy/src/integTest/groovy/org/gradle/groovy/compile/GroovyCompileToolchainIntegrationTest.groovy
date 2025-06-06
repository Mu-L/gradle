/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.groovy.compile

import org.gradle.api.JavaVersion
import org.gradle.integtests.fixtures.AvailableJavaHomes
import org.gradle.integtests.fixtures.MultiVersionIntegrationSpec
import org.gradle.integtests.fixtures.TargetCoverage
import org.gradle.integtests.fixtures.jvm.JavaToolchainFixture
import org.gradle.internal.jvm.Jvm
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.precondition.Requires
import org.gradle.test.preconditions.IntegTestPreconditions
import org.gradle.testing.fixture.GroovyCoverage
import org.gradle.util.internal.GroovyDependencyUtil
import org.gradle.util.internal.TextUtil
import org.junit.Assume

import static org.gradle.util.internal.GroovyDependencyUtil.groovyModuleDependency

@TargetCoverage({ GroovyCoverage.SINCE_3_0 })
class GroovyCompileToolchainIntegrationTest extends MultiVersionIntegrationSpec implements JavaToolchainFixture {

    def setup() {
        file("src/main/groovy/JavaThing.java") << "public class JavaThing {}"
        file("src/main/groovy/GroovyBar.groovy") << "public class GroovyBar { def bar() {} }"

        buildFile << """
            apply plugin: "groovy"
            ${mavenCentralRepository()}

            dependencies {
                implementation "${groovyModuleDependency("groovy", MultiVersionIntegrationSpec.version)}"
            }
        """
    }

    def "forkOptions #option is ignored for Groovy "() {
        def currentJdk = Jvm.current()
        def otherJdk = AvailableJavaHomes.getDifferentVersion()

        if (option == "executable") {
            buildFile << """
                compileGroovy {
                    options.fork = true
                    options.forkOptions.executable = "${TextUtil.normaliseFileSeparators(otherJdk.javaExecutable.absolutePath)}"
                }
            """
        } else {
            buildFile << """
                compileGroovy {
                    options.fork = true
                    options.forkOptions.javaHome = file("${TextUtil.normaliseFileSeparators(otherJdk.javaHome.absolutePath)}")
                }
            """
        }

        def groovyTarget = GroovyCoverage.getEffectiveTarget(versionNumber, currentJdk.javaVersion)

        when:
        withInstallations(otherJdk).run(":compileGroovy")

        then:
        executedAndNotSkipped(":compileGroovy")
        JavaVersion.forClass(groovyClassFile("JavaThing.class").bytes) == currentJdk.javaVersion
        JavaVersion.forClass(groovyClassFile("GroovyBar.class").bytes) == groovyTarget

        where:
        option << ["executable", "javaHome"]
    }

    def "uses #what toolchain #when for Groovy "() {
        def currentJdk = Jvm.current()
        def otherJdk = AvailableJavaHomes.differentVersion
        def selectJdk = { it == "other" ? otherJdk : it == "current" ? currentJdk : null }

        if (withTool != null) {
            configureTool(selectJdk(withTool))
        }
        if (withJavaExtension != null) {
            configureJavaPluginToolchainVersion(selectJdk(withJavaExtension))
        }

        def targetJdk = selectJdk(target)
        def groovyTarget = GroovyCoverage.getEffectiveTarget(versionNumber, targetJdk.javaVersion)

        when:
        withInstallations(currentJdk, otherJdk).run(":compileGroovy", "--info")

        then:
        executedAndNotSkipped(":compileGroovy")
        outputContains("Compiling with JDK Java compiler API")
        JavaVersion.forClass(groovyClassFile("JavaThing.class").bytes) == targetJdk.javaVersion
        JavaVersion.forClass(groovyClassFile("GroovyBar.class").bytes) == groovyTarget

        where:
        what             | when                         | withTool | withJavaExtension | target
        "current JVM"    | "when nothing is configured" | null     | null              | "current"
        "java extension" | "when configured"            | null     | "other"           | "other"
        "assigned tool"  | "when configured"            | "other"  | null              | "other"
        "assigned tool"  | "over java extension"        | "other"  | "current"         | "other"
    }

    def "up-to-date depends on the toolchain for Groovy "() {
        def currentJdk = Jvm.current()
        def otherJdk = AvailableJavaHomes.getDifferentVersion()

        buildFile << """
            compileGroovy {
                javaLauncher = javaToolchains.launcherFor {
                    languageVersion = JavaLanguageVersion.of(
                        providers.gradleProperty("changed").isPresent()
                            ? ${otherJdk.javaVersion.majorVersion}
                            : ${currentJdk.javaVersion.majorVersion}
                    )
                }
            }
        """

        when:
        withInstallations(currentJdk, otherJdk).run(":compileGroovy")
        then:
        executedAndNotSkipped(":compileGroovy")

        when:
        withInstallations(currentJdk, otherJdk).run(":compileGroovy")
        then:
        skipped(":compileGroovy")

        when:
        withInstallations(currentJdk, otherJdk).run(":compileGroovy", "-Pchanged", "--info")
        then:
        executedAndNotSkipped(":compileGroovy")
        outputContains("Value of input property 'groovyCompilerJvmVersion' has changed for task ':compileGroovy'")
        outputContains("Value of input property 'javaLauncher.metadata.languageVersion' has changed for task ':compileGroovy'")

        when:
        withInstallations(currentJdk, otherJdk).run(":compileGroovy", "-Pchanged")
        then:
        skipped(":compileGroovy")
    }

    @Requires(IntegTestPreconditions.Java21HomeAvailable)
    def 'source and target compatibility override toolchain (source #source, target #target) for Groovy '() {
        def jdk21 = AvailableJavaHomes.getJdk(JavaVersion.VERSION_21)

        buildFile << """
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }

            compileGroovy {
                ${source != 'none' ? "sourceCompatibility = JavaVersion.toVersion($source)" : ''}
                ${target != 'none' ? "targetCompatibility = JavaVersion.toVersion($target)" : ''}
                def projectSourceCompat = project.java.sourceCompatibility
                def projectTargetCompat = project.java.targetCompatibility
                doLast {
                    logger.lifecycle("project.sourceCompatibility = \$projectSourceCompat")
                    logger.lifecycle("project.targetCompatibility = \$projectTargetCompat")
                    logger.lifecycle("task.sourceCompatibility = \$sourceCompatibility")
                    logger.lifecycle("task.targetCompatibility = \$targetCompatibility")
                }
            }
        """

        when:
        withInstallations(jdk21).run(":compileGroovy")

        then:
        executedAndNotSkipped(":compileGroovy")

        outputContains("project.sourceCompatibility = 21")
        outputContains("project.targetCompatibility = 21")
        outputContains("task.sourceCompatibility = $sourceOut")
        outputContains("task.targetCompatibility = $targetOut")
        JavaVersion.forClass(groovyClassFile("JavaThing.class").bytes) == JavaVersion.toVersion(targetOut)
        JavaVersion.forClass(groovyClassFile("GroovyBar.class").bytes) == GroovyCoverage.getEffectiveTarget(versionNumber, JavaVersion.toVersion(targetOut))

        where:
        source | target | sourceOut | targetOut
        '17'   | '20'   | '17'      | '20'
        '17'   | 'none' | '17'      | '17'
        'none' | 'none' | '21'      | '21'
    }

    def "can compile source and run tests using Java #javaVersion for Groovy "() {
        // This condition can't be part of the `where` block because that is only evaluated for the first Groovy version
        Assume.assumeTrue(
            "groovy must support the Java version to test",
            GroovyCoverage.supportsJavaVersion(version, javaVersion)
        )
        def jdk = AvailableJavaHomes.getJdk(javaVersion)
        Assume.assumeTrue(jdk != null)

        configureJavaPluginToolchainVersion(jdk)

        buildFile("""
            dependencies {
                testImplementation "${GroovyDependencyUtil.spockModuleDependency("spock-core", versionNumber)}"
            }

            testing.suites.test.useJUnitJupiter()
        """)

        if (versionNumber.major > 4) {
            // Spock doesn't target Groovy 5.x yet
            buildFile("""
                tasks.compileTestGroovy.configure {
                    groovyOptions.forkOptions.jvmArgs += ["-Dspock.iKnowWhatImDoing.disableGroovyVersionCheck=true"]
                }
            """)
        }

        file("src/test/groovy/GroovySpec.groovy") << """
            class GroovySpec extends spock.lang.Specification {
                def test() {
                    given:
                    def v = System.getProperty("java.version")
                    println "Running Groovy test with Java version \$v"
                }
            }
        """

        def groovyTarget = GroovyCoverage.getEffectiveTarget(versionNumber, jdk.javaVersion)

        when:
        withInstallations(jdk).run(":test", "--info")

        then:
        executedAndNotSkipped(":test")
        outputContains("Running Groovy test with Java version ${jdk.javaVersion}")

        JavaVersion.forClass(groovyClassFile("JavaThing.class").bytes) == javaVersion
        JavaVersion.forClass(groovyClassFile("GroovyBar.class").bytes) == groovyTarget
        JavaVersion.forClass(classFile("groovy", "test", "GroovySpec.class").bytes) == groovyTarget

        where:
        javaVersion << JavaVersion.values().findAll { it >= JavaVersion.VERSION_1_8 }
    }

    private TestFile configureTool(Jvm jdk) {
        buildFile << """
            compileGroovy {
                javaLauncher = javaToolchains.launcherFor {
                    languageVersion = JavaLanguageVersion.of(${jdk.javaVersion.majorVersion})
                }
            }
        """
    }

}
