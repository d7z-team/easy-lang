package org.d7z.lang

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class LangRunnerTest {
    @Test
    fun test() {
        println(
            LangCompiler(
                """
                        'arch' + monitor.hostname  = 'arch linux'
                """.trimIndent()
            ).apply {
                println(this)
            }.build().execute(
                mapOf(
                    Pair("monitor.hostname", Pair(LangDataType.TEXT, "linux"))
                )
            ).second
        )
    }
}
