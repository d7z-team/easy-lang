import org.gradle.api.Project
import org.gradle.api.logging.Logging
import java.io.File
import java.util.Properties

/**
 * 版本实体
 */
interface VersionContext {
    /**
     * 根据名称获取版本号
     */
    operator fun get(key: String): String
}

private val logger = Logging.getLogger("buildSrc.versions")

private val versions = HashMap<File, VersionContext>()

private val contextVersion = Properties().apply {
    val filter: (Map.Entry<Any, Any>) -> Boolean = { it.key.toString().toLowerCase().contains("version") }
    putAll(System.getProperties().filter(filter)) // 规范化环境变量
    putAll(
        System.getenv().filter(filter)
            .map { it.key.toLowerCase().replace("_", ".") to it.value }
    ) // 规范化环境变量
}

internal class InternalVersionContext(private val path: File) : VersionContext {
    private val properties = Properties()

    init {
        val gradlePropFile = File(path, "gradle.properties")
        val versionPropFile = File(path, "version.properties")
        if (gradlePropFile.isFile) {
            properties.load(gradlePropFile.inputStream())
        }
        if (versionPropFile.isFile) {
            properties.load(versionPropFile.inputStream())
        }
    }

    override fun get(key: String): String {
        val property = contextVersion.getProperty(key)
        if (property != null && property.isNotBlank()) {
            logger.warn(
                "Found dependency \"{}\" with dynamic version constraints.Current version is \"{}\".",
                key,
                property
            )
            return property
        }
        return contextVersion.getOrElse(key, {
            properties.getOrElse(key, {
                logger.warn("Unable to find a valid version of $key.")
                ""
            })
        }).toString()
    }
}

fun Project.contextVersions(header: String = ""): VersionContext {
    return versions(this.rootProject, header)
}

fun versions(project: Project, header: String = ""): VersionContext {
    val data = versions.getOrPut(project.projectDir, {
        InternalVersionContext(project.projectDir)
    })
    if (header.isBlank()) {
        return data
    } else {
        return HeaderVersionContext(data, header)
    }
}

class HeaderVersionContext(private val child: VersionContext, private val header: String) : VersionContext {
    override fun get(key: String) = child.get("$header.$key")
}
