package com.github.davinkevin.podcastserver.service


import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import java.lang.RuntimeException

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
        val returnCode = processService.waitFor(process)

        /* Then */
        assertThat(returnCode.getOrNull()).isEqualTo(10)
        verify(process).waitFor()
    }

    @Test
    fun `should wait and return nothing if ends up in error`() {
        /* Given */
        val process = mock<Process>()
        doThrow(RuntimeException("error during waiting") ).whenever(process).waitFor()

        /* When */
        val returnCode = processService.waitFor(process)

        /* Then */
        assertThat(returnCode.isFailure).isTrue()
        verify(process).waitFor()
    }
}
