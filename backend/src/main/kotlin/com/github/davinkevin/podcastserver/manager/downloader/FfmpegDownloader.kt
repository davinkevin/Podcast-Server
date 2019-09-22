package com.github.davinkevin.podcastserver.manager.downloader


import arrow.core.Failure
import arrow.core.Try
import com.github.davinkevin.podcastserver.download.DownloadRepository
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.service.*
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.progress.ProgressListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock

@Scope(SCOPE_PROTOTYPE)
@Component
class FfmpegDownloader(
        downloadRepository: DownloadRepository,
        podcastServerParameters: PodcastServerParameters,
        template: MessagingTemplate,
        mimeTypeService: MimeTypeService,
        clock: Clock,
        val ffmpegService: FfmpegService,
        val processService: ProcessService
) : AbstractDownloader(downloadRepository, podcastServerParameters, template, mimeTypeService, clock) {

    private val log = LoggerFactory.getLogger(FfmpegDownloader::class.java)

    lateinit var process: Process
    private var globalDuration = 0.0
    private var alreadyDoneDuration = 0.0

    override fun download(): DownloadingItem {
        log.debug("Download {}", downloadingInformation.item.title)

        target = computeTargetFile(downloadingInformation)

        globalDuration = downloadingInformation.urls
                .map { ffmpegService.getDurationOf(it, downloadingInformation.userAgent) }
                .sum()

        val multiDownloads = downloadingInformation.urls.map { download(it) }
        val files = multiDownloads.flatMap { it.toList() }

        try {
            if (multiDownloads.any { it.isFailure() }) {
                throw RuntimeException("Error during download of a part",
                        (multiDownloads.first { it.isFailure() } as Failure).exception
                )
            }

            ffmpegService.concat(target!!, *files.toTypedArray())
        } catch (e: Exception) {
            throw e
        } finally {
            files.forEach { Try { Files.deleteIfExists(it) } }
        }

        if (downloadingInformation.item.status == Status.STARTED)
            finishDownload()

        return downloadingInformation.item
    }

    private fun download(url: String): Try<Path> {
        val duration = ffmpegService.getDurationOf(url, downloadingInformation.userAgent)

        val subTarget = generateTempFileNextTo(target!!)

        return Try {
            val command = FFmpegBuilder()
                    .setUserAgent(downloadingInformation.userAgent ?: UrlService.USER_AGENT_DESKTOP)
                    .addInput(url)
                    .addOutput(subTarget.toAbsolutePath().toString())
                    .setFormat("mp4")
                    .setAudioBitStreamFilter(FfmpegService.AUDIO_BITSTREAM_FILTER_AAC_ADTSTOASC)
                    .setVideoCodec(FfmpegService.CODEC_COPY)
                    .setAudioCodec(FfmpegService.CODEC_COPY)
                    .done()

            process = ffmpegService.download(url, command, handleProgression(alreadyDoneDuration, globalDuration))

            processService.waitFor(process)

            alreadyDoneDuration += duration
            subTarget
        }
                .fold ({ Files.deleteIfExists(subTarget); Try.raise(it) }, { Try.just(it) })

    }

    private fun handleProgression(alreadyDoneDuration: Double, globalDuration: Double) =
            ProgressListener{
                broadcastProgression(((it.out_time_ms.toFloat() + alreadyDoneDuration.toFloat()) / globalDuration.toFloat() * 100).toInt())
            }

    private fun broadcastProgression(progress: Int) {

        if (downloadingInformation.item.progression == progress) return

        downloadingInformation = downloadingInformation.progression(progress)
        log.debug("Progression : {}", downloadingInformation.item.progression)

        broadcast(downloadingInformation.item)
    }

    override fun pauseDownload() =
            try {
                val pauseProcess = processService.newProcessBuilder("kill", "-STOP", "" + processService.pidOf(process))
                processService.start(pauseProcess)
                super.pauseDownload()
            } catch (e: Exception) {
                log.error("Error during pause of process :", e)
                failDownload()
            }

    override fun restartDownload() =
            try {
                val restart = processService.newProcessBuilder("kill", "-SIGCONT", "" + processService.pidOf(process))
                processService.start(restart)
                downloadingInformation = downloadingInformation.status(Status.STARTED)
                saveStateOfItem(downloadingInformation.item)
                broadcast(downloadingInformation.item)
            } catch (e: Exception) {
                log.error("Error during restart of process :", e)
                failDownload()
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
            if (downloadingInformation.urls.map { it.toLowerCase() }.all { "m3u8" in it || "mp4" in it }) 10
            else Integer.MAX_VALUE
}

private fun <B> Try<B>.toList(): List<B> = this.fold( { listOf() }, {listOf(it)})
