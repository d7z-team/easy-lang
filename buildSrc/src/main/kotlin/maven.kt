import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension

fun PublishingExtension.includeRepositories(project: Project) {
    repositories {
        mavenLocal()
        maven {
            name = "SnapshotsRepo"
            url = project.uri("https://repo.open-edgn.cn/maven/")
            credentials {
                username = (project.findProperty("edgn.m2.user") ?: System.getenv("USERNAME"))?.toString()
                password = (project.findProperty("edgn.m2.key") ?: System.getenv("TOKEN"))?.toString()
            }
        }
    }
}
