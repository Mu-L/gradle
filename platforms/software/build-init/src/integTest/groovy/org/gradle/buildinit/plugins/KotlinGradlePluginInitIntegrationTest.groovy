/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.buildinit.plugins

import org.gradle.buildinit.plugins.fixtures.ScriptDslFixture
import org.gradle.test.fixtures.file.LeaksFileHandles
import org.gradle.test.precondition.Requires
import org.gradle.test.preconditions.IntegTestPreconditions
import org.gradle.test.preconditions.UnitTestPreconditions
import spock.lang.Issue

import static org.gradle.buildinit.plugins.GroovyGradlePluginInitIntegrationTest.NOT_RUNNING_ON_EMBEDDED_EXECUTER_REASON
import static org.gradle.buildinit.plugins.internal.modifiers.BuildInitDsl.KOTLIN
import static org.hamcrest.CoreMatchers.allOf
import static org.hamcrest.CoreMatchers.not

@LeaksFileHandles
@Requires(value = UnitTestPreconditions.KotlinSupportedJdk)
class KotlinGradlePluginInitIntegrationTest extends AbstractInitIntegrationSpec {

    @Override
    String subprojectName() { 'plugin' }

    def "defaults to kotlin build scripts"() {
        when:
        run ('init', '--type', 'kotlin-gradle-plugin')

        then:
        dslFixtureFor(KOTLIN).assertGradleFilesGenerated()
    }

    @Requires(value = IntegTestPreconditions.NotEmbeddedExecutor, reason = NOT_RUNNING_ON_EMBEDDED_EXECUTER_REASON )
    def "creates sample source if no source present with #scriptDsl build scripts"() {
        def dslFixture = dslFixtureFor(scriptDsl)

        when:
        run('init', '--type', 'kotlin-gradle-plugin', '--dsl', scriptDsl.id)

        then:
        subprojectDir.file("src/main/kotlin").assertHasDescendants("org/example/SomeThingPlugin.kt")
        subprojectDir.file("src/test/kotlin").assertHasDescendants("org/example/SomeThingPluginTest.kt")
        subprojectDir.file("src/functionalTest/kotlin").assertHasDescendants("org/example/SomeThingPluginFunctionalTest.kt")

        and:
        commonJvmFilesGenerated(scriptDsl)
        dslFixture.assertDoesNotUseTestSuites()

        when:
        run("build")

        then:
        assertTestPassed("org.example.SomeThingPluginTest", "plugin registers task")
        assertFunctionalTestPassed("org.example.SomeThingPluginFunctionalTest", "can run task")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Requires(value = IntegTestPreconditions.NotEmbeddedExecutor, reason = NOT_RUNNING_ON_EMBEDDED_EXECUTER_REASON)
    def "creates build using test suites with #scriptDsl build scripts when using --incubating"() {
        def dslFixture = dslFixtureFor(scriptDsl)

        when:
        run('init', '--type', 'kotlin-gradle-plugin', '--dsl', scriptDsl.id, '--incubating')

        then:
        subprojectDir.file("src/main/kotlin").assertHasDescendants("org/example/SomeThingPlugin.kt")
        subprojectDir.file("src/test/kotlin").assertHasDescendants("org/example/SomeThingPluginTest.kt")
        subprojectDir.file("src/functionalTest/kotlin").assertHasDescendants("org/example/SomeThingPluginFunctionalTest.kt")

        and:
        commonJvmFilesGenerated(scriptDsl)
        dslFixture.assertHasTestSuite("test")
        dslFixture.assertHasTestSuite("functionalTest")

        when:
        run("build")

        then:
        assertTestPassed("org.example.SomeThingPluginTest", "plugin registers task")
        assertFunctionalTestPassed("org.example.SomeThingPluginFunctionalTest", "can run task")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Requires(value = IntegTestPreconditions.NotEmbeddedExecutor, reason = NOT_RUNNING_ON_EMBEDDED_EXECUTER_REASON)
    def "creates with gradle.properties when using #scriptDsl build scripts with --incubating"() {
        when:
        run('init', '--type', 'kotlin-gradle-plugin', '--dsl', scriptDsl.id, '--incubating')

        then:
        gradlePropertiesGenerated {
            assertCachingEnabled()
            assertParallelEnabled()
            assertConfigurationCacheEnabled()
        }

        when:
        run("build")

        then:
        assertTestPassed("org.example.SomeThingPluginTest", "plugin registers task")
        assertFunctionalTestPassed("org.example.SomeThingPluginFunctionalTest", "can run task")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Issue("https://github.com/gradle/gradle/issues/18206")
    @Requires(value = IntegTestPreconditions.NotEmbeddedExecutor, reason = NOT_RUNNING_ON_EMBEDDED_EXECUTER_REASON)
    def "re-running check succeeds with #scriptDsl"() {
        given:
        run('init', '--type', 'kotlin-gradle-plugin', '--dsl', scriptDsl.id)

        when:
        run('check')

        then:
        assertTestPassed("org.example.SomeThingPluginTest", "plugin registers task")
        assertFunctionalTestPassed("org.example.SomeThingPluginFunctionalTest", "can run task")

        when:
        run('check', '--rerun-tasks')

        then:
        assertTestPassed("org.example.SomeThingPluginTest", "plugin registers task")
        assertFunctionalTestPassed("org.example.SomeThingPluginFunctionalTest", "can run task")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Issue("https://github.com/gradle/gradle/issues/17137")
    @Requires(value = IntegTestPreconditions.NotEmbeddedExecutor, reason = NOT_RUNNING_ON_EMBEDDED_EXECUTER_REASON)
    def "does not contain junit specific kotlin test dependencies"() {
        when:
        run ('init', '--type', 'kotlin-gradle-plugin')

        then:
        def dslFixture = dslFixtureFor(KOTLIN)
        dslFixture.assertGradleFilesGenerated()
        dslFixture.buildFile.assertContents(
            allOf(
                not(dslFixture.containsConfigurationDependencyNotation('testImplementation', '"org.jetbrains.kotlin:kotlin-test-junit5"')),
                not(dslFixture.containsConfigurationDependencyNotation('testImplementation', '"org.jetbrains.kotlin:kotlin-test-junit"')),
                dslFixture.containsConfigurationDependencyNotation('testImplementation', '"org.jetbrains.kotlin:kotlin-test"')
            )
        )

        when:
        run('check', '--rerun-tasks')

        then:
        assertTestPassed("org.example.SomeThingPluginTest", "plugin registers task")
        assertFunctionalTestPassed("org.example.SomeThingPluginFunctionalTest", "can run task")

    }
}
