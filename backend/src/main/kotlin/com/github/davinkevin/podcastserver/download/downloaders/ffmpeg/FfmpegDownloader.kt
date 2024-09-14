package com.github.davinkevin.podcastserver.download.downloaders.ffmpeg

import com.github.davinkevin.podcastserver.download.ItemDownloadManager
import com.github.davinkevin.podcastserver.download.downloaders.Downloader
import com.github.davinkevin.podcastserver.download.downloaders.DownloaderHelper
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingInformation
import com.github.davinkevin.podcastserver.download.downloaders.DownloadingItem
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.ffmpeg.FfmpegService
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.progress.ProgressListener
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

class FfmpegDownloader(
    private val state: DownloaderHelper,
    private val ffmpegService: FfmpegService,
    private val processService: ProcessService,
) : Downloader {

    private val log = LoggerFactory.getLogger(FfmpegDownloader::class.java)

    override val downloadingInformation: DownloadingInformation
        get() = state.info

    lateinit var process: Process
    private var globalDuration = 0.0
    private var alreadyDoneDuration = 0.0

    override fun download(): DownloadingItem {
        log.debug("Download {}", state.info.item.title)

        state.target = state.computeTargetFile(state.info)

        globalDuration = state.info.urls
            .sumOf { ffmpegService.getDurationOf(it.toASCIIString(), state.info.userAgent) }

        val multiDownloads = state.info.urls.map { download(it.toASCIIString()) }

        if (multiDownloads.any { it.isFailure }) {
            val cause = multiDownloads.first { it.isFailure }.exceptionOrNull()
            throw RuntimeException("Error during download of a part", cause)
        }

        runCatching {
            ffmpegService.concat(state.target, *multiDownloads.map { it.getOrNull()!! }.toTypedArray())
        }

        multiDownloads
            .mapNotNull { it.getOrNull() }
            .forEach { runCatching { Files.deleteIfExists(it) } }

        if (downloadingInformation.item.status == Status.STARTED) {
            finishDownload()
        }

        return downloadingInformation.item
    }

    private fun download(url: String): Result<Path> {
        val duration = ffmpegService.getDurationOf(url, state.info.userAgent)

        val subTarget = Files.createTempFile("podcast-server", state.info.item.id.toString())
        val userAgent = state.info.userAgent ?: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36"
        val command = FFmpegBuilder()
            .addUserAgent(userAgent)
            .addInput(url)
            .addOutput(subTarget.toAbsolutePath().toString())
            .setFormat("mp4")
            .setAudioBitStreamFilter(FfmpegService.AUDIO_BITSTREAM_FILTER_AAC_ADTSTOASC)
            .setVideoCodec(FfmpegService.CODEC_COPY)
            .setAudioCodec(FfmpegService.CODEC_COPY)
            .done()

        return Result.runCatching {
            process = ffmpegService.download(url, command, handleProgression(alreadyDoneDuration, globalDuration))
            processService.waitFor(process)
            alreadyDoneDuration += duration
            subTarget
        }
            .onFailure { Files.deleteIfExists(subTarget) }

    }

    private fun handleProgression(alreadyDoneDuration: Double, globalDuration: Double) =
        ProgressListener{
            broadcastProgression(((it.out_time_ns.toFloat() + alreadyDoneDuration.toFloat()) / globalDuration.toFloat() * 100).toInt())
        }

    private fun broadcastProgression(progress: Int) {

        if (state.info.item.progression == progress) return

        state.info = state.info.progression(progress)
        log.debug("Progression : {}", state.info.item.progression)

        state.broadcast(state.info)
    }

    override fun stopDownload() {
        try {
            process.destroy()
            state.stopDownload()
        } catch (e: Exception) {
            log.error("Error during stop of process :", e)
            state.failDownload()
        }
    }

    override fun startDownload() = state.startDownload(this)
    override fun failDownload() = state.failDownload()
    override fun finishDownload() = state.finishDownload()
}

private fun FFmpegBuilder.addUserAgent(userAgent: String) = addExtraArgs("-headers", "User-Agent: $userAgent")