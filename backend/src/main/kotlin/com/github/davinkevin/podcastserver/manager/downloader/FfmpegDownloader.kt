package com.github.davinkevin.podcastserver.manager.downloader


import arrow.core.Failure
import arrow.core.Try
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.entity.Status
import com.github.davinkevin.podcastserver.service.*
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.progress.ProgressListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Scope(SCOPE_PROTOTYPE)
@Component
class FfmpegDownloader(
        itemRepository: ItemRepository,
        podcastRepository: PodcastRepository,
        podcastServerParameters: PodcastServerParameters,
        template: MessagingTemplate,
        mimeTypeService: MimeTypeService,
        val ffmpegService: FfmpegService,
        val processService: ProcessService
) : AbstractDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService) {

    private val log = LoggerFactory.getLogger(FfmpegDownloader::class.java)

    lateinit var process: Process
    private var globalDuration = 0.0
    private var alreadyDoneDuration = 0.0

    override fun download(): Item {
        log.debug("Download {}", item.title)

        target = getTargetFile(downloadingItem.item)

        globalDuration = downloadingItem.urls
                .map { ffmpegService.getDurationOf(it, downloadingItem.userAgent) }
                .sum()

        val multiDownloads = downloadingItem.urls.map { download(it) }
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

        if (item.status == Status.STARTED)
            finishDownload()

        return downloadingItem.item
    }

    private fun download(url: String): Try<Path> {
        val duration = ffmpegService.getDurationOf(url, downloadingItem.userAgent)

        val subTarget = generateTempFileNextTo(target!!)

        return Try {
            val command = FFmpegBuilder()
                    .setUserAgent(downloadingItem.userAgent ?: UrlService.USER_AGENT_DESKTOP)
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

        if (item.progression == progress) return

        item.progression = progress
        log.debug("Progression : {}", item.progression)
        convertAndSaveBroadcast()
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
                item.status = Status.STARTED
                saveSyncWithPodcast()
                convertAndSaveBroadcast()
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

    override fun compatibility(downloadingItem: DownloadingItem) =
            if (downloadingItem.urls.map { it.toLowerCase() }.all { "m3u8" in it || "mp4" in it }) 10
            else Integer.MAX_VALUE
}

private fun <B> Try<B>.toList(): List<B> = this.fold( { listOf() }, {listOf(it)})