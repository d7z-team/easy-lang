package com.github.template

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MainTest {

    @Test
    fun testTrue() {
        assertEquals(1 + 1, 2)
    }

    @Test
    fun testMain() {
        main()
    }
}
