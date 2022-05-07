import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `maven-publish`
    application
}
java.sourceCompatibility = JavaVersion.VERSION_11

val compileKotlin: KotlinCompile by tasks
val compileJava: JavaCompile by tasks
compileKotlin.destinationDirectory.set(compileJava.destinationDirectory.get())

java {
    modularity.inferModulePath.set(true)
}

application {
    // 启动类配置
    mainModule.set("gradle.kotlin.template")
    mainClass.set("com.github.template.MainKt")
}

val versions = contextVersions("version")

dependencies {
    implementation(kotlin("reflect"))
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter:${versions["junit.jupiter"]}")
    testImplementation("org.junit.platform:junit-platform-launcher:${versions["junit.launcher"]}")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}
val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = (parent ?: rootProject).group.toString()
            version = (parent ?: rootProject).version.toString()
            artifactId = project.name
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
    includeRepositories(project)
}
