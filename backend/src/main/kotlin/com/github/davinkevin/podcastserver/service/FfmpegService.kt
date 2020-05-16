package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.CustomRunProcessFunc
import com.github.davinkevin.podcastserver.utils.custom.ffmpeg.ProcessListener
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.progress.ProgressListener
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.util.FileSystemUtils
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.TimeUnit

/**
 * Created by kevin on 19/07/2014 for Podcast Server
 */
class FfmpegService(
        private val runProcessFunc: CustomRunProcessFunc,
        private val ffmpegExecutor: FFmpegExecutor,
        private val ffprobe: FFprobe
) {

    private val log = LoggerFactory.getLogger(FfmpegService::class.java)
    private val separator = System.getProperty("line.separator")

    /* Concat files */
    fun concat(target: Path, vararg files: Path) {

        val tmpFolder = Files.createTempDirectory("podcast-server-ffmpeg-download-")
        val listOfFiles = Files.createTempFile(tmpFolder, "ffmpeg-list-", ".txt")

        Result.runCatching {
            Files.deleteIfExists(target)

            val filesStrings = files
                    .map { Files.move(it, Files.createTempFile(tmpFolder, "ffmpeg-file-", ""), StandardCopyOption.REPLACE_EXISTING) }
                    .map { f -> f.fileName.toString() }
                    .map { p -> "file '$p'" }
                    .reduce { acc, s -> "$acc$separator$s" }

            Files.write(listOfFiles, filesStrings.toByteArray())

            val builder = FFmpegBuilder()
                    .setInput(listOfFiles.toAbsolutePath().toString())
                    .setFormat(FORMAT_CONCAT)
                    .addOutput(target.toAbsolutePath().toString())
                    .setAudioCodec(CODEC_COPY)
                    .setVideoCodec(CODEC_COPY)
                    .setFormat("mp4")
                    .done()

            ffmpegExecutor.createJob(builder).run()
        }
                .onFailure { log.error("Error during Ffmpeg conversion", it) }

        FileSystemUtils.deleteRecursively(tmpFolder)
    }

    /* Merge Audio and Video Files */
    fun mergeAudioAndVideo(videoFile: Path, audioFile: Path, dest: Path): Path {

        val convertedAudio = convert(audioFile, audioFile.resolveSibling(changeExtension(audioFile, "aac")))

        val tmpFile = generateTempFileFor(dest, videoFile)

        val builder = FFmpegBuilder()
                .setInput(convertedAudio.toAbsolutePath().toString())
                .addInput(videoFile.toAbsolutePath().toString())
                .addOutput(tmpFile.toAbsolutePath().toString())
                .setAudioBitStreamFilter(AUDIO_BITSTREAM_FILTER_AAC_ADTSTOASC)
                .setAudioCodec(CODEC_COPY)
                .setVideoCodec(CODEC_COPY)
                .done()

        ffmpegExecutor.createJob(builder).run()

        Files.deleteIfExists(convertedAudio)
        Files.move(tmpFile, dest, StandardCopyOption.REPLACE_EXISTING)

        return dest
    }

    private fun convert(source: Path, dest: Path): Path {
        val converter = FFmpegBuilder()
                .setInput(source.toAbsolutePath().toString())
                .addOutput(dest.toAbsolutePath().toString())
                .disableVideo()
                .done()

        ffmpegExecutor.createJob(converter).run()

        return dest
    }

    private fun generateTempFileFor(dest: Path, video: Path): Path {
        val extension = FilenameUtils.getExtension(video.fileName.toString())!!
        return Files.createTempFile(dest.parent, dest.fileName.toString(), ".$extension")
    }

    private fun changeExtension(audioFile: Path, ext: String): Path {
        val newName = FilenameUtils.getBaseName(audioFile.fileName.toString()) + "." + ext
        return audioFile.resolveSibling(newName)
    }

    /* Get duration of a File */
    fun getDurationOf(url: String, userAgent: String?): Double {

        val duration = supplyAsync {
            Result.runCatching { ffprobe.probe(url, userAgent).getFormat().duration }
                    .map { d -> d * 1000000 }
                    .getOrElse { throw RuntimeException(it) }
        }

        return Result
                .runCatching { duration.get(60, TimeUnit.SECONDS) }
                .getOrElse {
                    duration.cancel(true)
                    throw RuntimeException("Error during probe operation", it)
                }
    }

    fun download(url: String, ffmpegBuilder: FFmpegBuilder, progressListener: ProgressListener): Process {
        val pl = ProcessListener(url)
        runProcessFunc.add(pl)

        runAsync(ffmpegExecutor.createJob(ffmpegBuilder, progressListener))

        return Result.runCatching { pl.process.get(2, TimeUnit.SECONDS) }
                .getOrElse {
                    pl.process.cancel(true)
                    log.error("error during download", it)
                    throw it
                }
    }

    companion object {
        val AUDIO_BITSTREAM_FILTER_AAC_ADTSTOASC = "aac_adtstoasc"
        val CODEC_COPY = "copy"
        private val FORMAT_CONCAT = "concat"
    }
}
