package com.github.davinkevin.podcastserver.service.ffmpeg

import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.CustomRunProcessFunc
import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.ProcessListener
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.job.FFmpegJob
import net.bramp.ffmpeg.probe.FFmpegFormat
import net.bramp.ffmpeg.probe.FFmpegProbeResult
import net.bramp.ffmpeg.progress.ProgressListener
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.mockito.*
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.util.concurrent.TimeoutException

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
        val input1 = Files.createTempFile("should_concat_files_1", ".mp4")
        val input2 = Files.createTempFile("should_concat_files_2", ".mp4")
        val input3 = Files.createTempFile("should_concat_files_3", ".mp4")

        /* When */
        ffmpegService.concat(output, input1, input2, input3)

        /* Then */
        verify(ffmpegExecutor).createJob(executorBuilderCaptor.capture())
        verify(job).run()
        assertThat(executorBuilderCaptor.value.build()).contains(
                "-f", "concat",
                "-i",
                "-vcodec", "copy",
                "-acodec", "copy",
                "/tmp/output.mp4"
        )
        assertThat(
                executorBuilderCaptor.value.build()
                        .any { it.contains("ffmpeg-list-") && it.endsWith(".txt") }
        ).isTrue
    }

    @Test
    fun `should catch error if problem`() {
        /* Given */
        val tmp = Paths.get("/tmp")
        whenever(ffmpegExecutor.createJob(any())).then { throw RuntimeException() }
        val output = tmp.resolve("output.mp4")
        val input1 = Files.createTempFile("should_concat_files_1", ".mp4")
        val input2 = Files.createTempFile("should_concat_files_2", ".mp4")

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
    @Disabled
    fun `should not merge if folder is read only`(@TempDir dir: Path) {
        /* Given */
        val subDir = dir.resolve("readonly")
        val readOnlyFolder = Files.createDirectory(subDir)
        Files.setPosixFilePermissions(readOnlyFolder, setOf(OWNER_READ))

        val video = readOnlyFolder.resolve("bar.mp4")
        val audio = readOnlyFolder.resolve("bar.webm")
        val dest = readOnlyFolder.resolve("foo.mp4")

        val job: FFmpegJob = mock()
        whenever(ffmpegExecutor.createJob(any())).thenReturn(job)

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
                .isInstanceOf(TimeoutException::class.java)
    }

}
