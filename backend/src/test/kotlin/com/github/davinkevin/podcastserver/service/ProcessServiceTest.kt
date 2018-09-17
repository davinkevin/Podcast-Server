package com.github.davinkevin.podcastserver.service


import arrow.core.getOrElse
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.verify

/**
 * Created by kevin on 31/03/2016 for Podcast Server
 */
class ProcessServiceTest {

    val processService = ProcessService()

    @Test
    fun `should create process with varargs`() {
        /* Given */
        /* When */
        val processBuilder = processService.newProcessBuilder("foo", "bar")

        /* Then */
        assertThat(processBuilder.command()).contains("foo", "bar")
    }

    @Test
    fun `should get pid`() {
        /* Given */
        val ls = processService.newProcessBuilder("ls", "-al").start()

        /* When */
        val pid = processService.pidOf(ls)

        /* Then */
        assertThat(pid).isGreaterThan(0)
    }

    @Test
    fun `should return invalid value if not on unix`() {
        /* Given */
        val process = mock<Process>()

        /* When */
        val pid = processService.pidOf(process)

        /* Then */
        assertThat(pid).isEqualTo(-1)
    }

    @Test
    fun `should start a process`() {
        /* Given */
        val processBuilder = ProcessBuilder("echo", "foo")

        /* When */
        val aProcess = processService.start(processBuilder)

        /* Then */
        assertThat(aProcess).isNotNull()
    }

    @Test
    fun `should wait for process`() {
        /* Given */
        val process = mock<Process>()
        whenever(process.waitFor()).thenReturn(10)

        /* When */
        val tryProcess = processService.waitFor(process)

        /* Then */
        assertThat(tryProcess.getOrElse { throw RuntimeException("No process found") }).isEqualTo(10)
        verify(process).waitFor()
    }
}
