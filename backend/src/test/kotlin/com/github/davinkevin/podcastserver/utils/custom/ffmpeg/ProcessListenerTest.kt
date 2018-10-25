package com.github.davinkevin.podcastserver.utils.custom.ffmpeg

import com.jayway.awaitility.Awaitility.await
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

internal class ProcessListenerTest {

    @Test
    fun `should wait if no process until one is present`() {
        /* Given */
        val pl = ProcessListener("foo")
        val aProcess = Mockito.mock(Process::class.java)
        var process: Process? = null

        /* When */
        CompletableFuture.runAsync { process = pl.process.get() }
        MILLISECONDS.sleep(200)
        pl.withProcess(aProcess)

        /* Then */
        await().atMost(5, SECONDS).until {
            assertThat(process).isNotNull().isSameAs(aProcess)
        }
    }

}