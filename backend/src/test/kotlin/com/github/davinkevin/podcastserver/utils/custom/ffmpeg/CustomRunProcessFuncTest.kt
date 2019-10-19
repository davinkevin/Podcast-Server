package com.github.davinkevin.podcastserver.utils.custom.ffmpeg

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CustomRunProcessFuncTest {

    @Test
    fun `should get process from listener`() {
        /* Given */
        val cp = CustomRunProcessFunc()
        val pl = ProcessListener("anUrl")

        /* When */
        val p = (cp + pl)
                .run(listOf("/bin/bash", "anUrl", "Foo", "Bar"))

        /* Then */
        assertThat(p).isSameAs(pl.process.get())
    }
}
