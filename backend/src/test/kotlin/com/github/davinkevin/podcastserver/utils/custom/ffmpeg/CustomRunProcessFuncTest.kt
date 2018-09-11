package com.github.davinkevin.podcastserver.utils.custom.ffmpeg

import io.vavr.API
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test

internal class CustomRunProcessFuncTest {

    @Test
    fun `should get process from listener`() {
        /* Given */
        val cp = CustomRunProcessFunc()
        val pl = ProcessListener("anUrl")

        /* When */
        val p = cp.plus(pl).run(API.List("/bin/bash", "anUrl", "Foo", "Bar").toJavaList())

        /* Then */
        assertThat(p).isSameAs(pl.process)
        assertThat(pl.findProcess().get()).isSameAs(p)
    }
}