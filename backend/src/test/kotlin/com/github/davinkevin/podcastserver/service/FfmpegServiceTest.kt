package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.CustomRunProcessFunc
import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.ProcessListener
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.job.FFmpegJob
import net.bramp.ffmpeg.probe.FFmpegFormat
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.progress.ProgressListener
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by kevin on 20/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class FfmpegServiceTest {

    @Mock lateinit var runFunc: CustomRunProcessFunc
    @Mock lateinit var ffprobe: FFprobe
    @Mock lateinit var ffmpegExecutor: FFmpegExecutor
    @InjectMocks lateinit var ffmpegService: FfmpegService

    @Captor lateinit var executorBuilderCaptor: ArgumentCaptor<FFmpegBuilder>

    @Test
    fun `should concat files`() {
        /* Given */
        val job: FFmpegJob = mock()
        whenever(ffmpegExecutor.createJob(any())).thenReturn(job)
        val output = Paths.get("/tmp/output.mp4")
        val input1 = Paths.get("/tmp/input1.mp4")
        val input2 = Paths.get("/tmp/input2.mp4")
        val input3 = Paths.get("/tmp/input3.mp4")

        /* When */
        ffmpegService.concat(output, input1, input2, input3)

        /* Then */
        verify(ffmpegExecutor, times(1)).createJob(executorBuilderCaptor.capture())
        verify(job, times(1)).run()
        assertThat(executorBuilderCaptor.value.build()).contains(
                "-f", "concat",
                "-i",
                "-vcodec", "copy",
                "-acodec", "copy",
                "/tmp/output.mp4"
        )
        assertThat(
                executorBuilderCaptor.value.build()
                        .any { it.startsWith("/tmp/ffmpeg-list-") && it.endsWith(".txt") }
        ).isTrue()
    }

    @Test
    fun `should catch error if problem`() {
        /* Given */
        val tmp = Paths.get("/tmp")
        whenever(ffmpegExecutor.createJob(any())).then { throw RuntimeException() }
        val output = tmp.resolve("output.mp4")
        val input1 = tmp.resolve("input1.mp4")
        val input2 = tmp.resolve("input2.mp4")

        /* When */
        ffmpegService.concat(output, input1, input2)

        /* Then */
        val files = Files.newDirectoryStream(tmp).use {
            it.filter { it.startsWith("ffmpeg-list-") }
        }
        assertThat(files).isEmpty()
    }

    @Test
    fun `should merge audio and video`() {
        /* Given */
        val video = Paths.get("/tmp/bar.mp4")
        val audio = Paths.get("/tmp/bar.webm")
        val destination = Paths.get("/tmp/foo.mp4")

        val job: FFmpegJob = mock()
        whenever(ffmpegExecutor.createJob(any())).thenReturn(job)

        /* When */
        val generatedFile = ffmpegService.mergeAudioAndVideo(video, audio, destination)

        /* Then */
        assertThat(generatedFile).isEqualTo(destination)
    }

    @Test
    fun `should not merge if folder is read only`() {
        /* Given */
        val video = Paths.get("/tmp/bar.mp4")
        val audio = Paths.get("/tmp/bar.webm")
        val dest = Paths.get("/foo.mp4")

        val job: FFmpegJob = mock()
        whenever(ffmpegExecutor.createJob(ArgumentMatchers.any())).thenReturn(job)

        /* When */
        assertThatThrownBy { ffmpegService.mergeAudioAndVideo(video, audio, dest) }

        /* Then */
                .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `should get duration`() {
        /* Given */
        val result = FFmpegProbeResult()
        result.format = FFmpegFormat()
        result.format.duration = 987.0
        whenever(ffprobe.probe("foo", "bar")).thenReturn(result)

        /* When */
        val duration = ffmpegService.getDurationOf("foo", "bar")

        /* Then */
        assertThat(duration).isEqualTo((987 * 1000000).toDouble())
    }

    @Test
    fun `should throw exception if ffprobe problem`() {
        /* Given */
        doThrow(IOException::class.java).`when`<FFprobe>(ffprobe).probe(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())

        /* When */
        assertThatThrownBy { ffmpegService.getDurationOf("foo", "bar") }

        /* Then */
                .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    fun `should download with ffmpeg`() {
        /* Given */
        val job: FFmpegJob = mock()
        whenever(runFunc.add(any())).then {
            val pl = it.getArgument<ProcessListener>(0)
            pl.withProcess(mock())
            runFunc
        }
        whenever(ffmpegExecutor.createJob(ArgumentMatchers.any(), ArgumentMatchers.any())).then { job }

        /* When */
        val p = ffmpegService.download("foo", FFmpegBuilder(), ProgressListener {})

        /* Then */
        assertThat(p).isNotNull()
    }

    @Test
    fun `should propagate error when happening in download`() {
        /* Given */
        val job: FFmpegJob = mock()
        whenever(ffmpegExecutor.createJob(ArgumentMatchers.any(), ArgumentMatchers.any())).then { job }

        /* When */
        assertThatThrownBy { ffmpegService.download("foo", FFmpegBuilder(), ProgressListener {}) }

        /* Then */
                .isInstanceOf(RuntimeException::class.java)
    }

}
