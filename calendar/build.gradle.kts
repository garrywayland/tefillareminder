plugins { id("org.jetbrains.kotlin.jvm") }
dependencies {
    api("com.kosherjava:zmanim:2.5.0")
    testImplementation(kotlin("test"))
}
tasks.test { useJUnitPlatform(); testLogging { events("passed", "failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL } }
