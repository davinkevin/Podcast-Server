package com.github.davinkevin.podcastserver.manager.downloader


import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.messaging.MessagingTemplate
import com.github.davinkevin.podcastserver.service.ffmpeg.FfmpegService
import com.github.davinkevin.podcastserver.service.ProcessService
import com.github.davinkevin.podcastserver.service.storage.FileStorageService
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.progress.ProgressListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.util.*

@Scope(SCOPE_PROTOTYPE)
@Component
class FfmpegDownloader(
    downloadRepository: DownloadRepository,
    template: MessagingTemplate,
    clock: Clock,
    file: FileStorageService,
    val ffmpegService: FfmpegService,
    val processService: ProcessService
) : AbstractDownloader(downloadRepository, template, clock, file) {

    private val log = LoggerFactory.getLogger(FfmpegDownloader::class.java)

    lateinit var process: Process
    private var globalDuration = 0.0
    private var alreadyDoneDuration = 0.0

    override fun download(): DownloadingItem {
        log.debug("Download {}", downloadingInformation.item.title)

        target = computeTargetFile(downloadingInformation)

        globalDuration = downloadingInformation.urls
                .sumOf { ffmpegService.getDurationOf(it.toASCIIString(), downloadingInformation.userAgent) }

        val multiDownloads = downloadingInformation.urls.map { download(it.toASCIIString()) }

        Result.runCatching {
            if (multiDownloads.any { it.isFailure }) {
                val cause = multiDownloads.first { it.isFailure }.exceptionOrNull()
                throw RuntimeException("Error during download of a part", cause)
            }

            ffmpegService.concat(
                target,
                    *multiDownloads.map { it.getOrNull()!! }.toTypedArray()
            )
        }

        multiDownloads
                .filter { it.isSuccess }
                .map { it.getOrNull()!! }
                .forEach { Result.runCatching { Files.deleteIfExists(it) } }

        if (downloadingInformation.item.status == Status.STARTED) {
            finishDownload()
        }

        return downloadingInformation.item
    }

    private fun download(url: String): Result<Path> {
        val duration = ffmpegService.getDurationOf(url, downloadingInformation.userAgent)

        val subTarget = Files.createTempFile("podcast-server", downloadingInformation.item.id.toString())
        val userAgent = downloadingInformation.userAgent ?: "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36"
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

        if (downloadingInformation.item.progression == progress) return

        downloadingInformation = downloadingInformation.progression(progress)
        log.debug("Progression : {}", downloadingInformation.item.progression)

        broadcast(downloadingInformation.item)
    }

    override fun stopDownload() {
        try {
            process.destroy()
            super.stopDownload()
        } catch (e: Exception) {
            log.error("Error during stop of process :", e)
            failDownload()
        }
    }

    override fun compatibility(downloadingInformation: DownloadingInformation) =
            if (downloadingInformation.urls.map { it.toASCIIString().lowercase(Locale.getDefault()) }.all { "m3u8" in it || "mp4" in it }) 10
            else Integer.MAX_VALUE
}

private fun FFmpegBuilder.addUserAgent(userAgent: String) = addExtraArgs("-headers", "User-Agent: $userAgent")
