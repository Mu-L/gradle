plugins {
    id("gradlebuild.distribution.api-java")
}

description = """Generalized test infrastructure to support executing tests in test workers."""

gradleModule {
    targetRuntimes {
        usedInWorkers = true
    }
}

dependencies {
    api(projects.baseServices)
    api(projects.concurrent)
    api(projects.stdlibJavaExtensions)
    api(projects.messaging)
    api(projects.serialization)
    api(projects.time)
    api(projects.workerMain)

    api(libs.jspecify)

    implementation(projects.io)
    implementation(projects.serviceLookup)
    implementation(projects.serviceProvider)
    implementation(projects.serviceRegistryBuilder)

    implementation(libs.commonsLang)
    implementation(libs.slf4jApi)

    testImplementation(projects.serviceRegistryImpl)
    testImplementation(libs.commonsIo)
    testImplementation(testFixtures(projects.time))
    testImplementation(testFixtures(projects.serialization))
    testImplementation(testFixtures(projects.time))

    integTestDistributionRuntimeOnly(projects.distributionsCore)
}

packageCycles {
    excludePatterns.add("org/gradle/api/internal/tasks/testing/**")
}

tasks.isolatedProjectsIntegTest {
    enabled = false
}
