package com.github.davinkevin.podcastserver.manager.downloader

import arrow.core.Try
import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.entity.Item
import com.github.davinkevin.podcastserver.service.MessagingTemplate
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.service.properties.ExternalTools
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.sapher.youtubedl.DownloadProgressCallback
import com.sapher.youtubedl.YoutubeDL
import com.sapher.youtubedl.YoutubeDLRequest
import com.sapher.youtubedl.YoutubeDLResponse
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.PodcastRepository
import org.apache.commons.io.FilenameUtils.getExtension
import org.apache.commons.io.FilenameUtils.removeExtension
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.roundToInt
import kotlin.streams.asSequence

/**
 * Created by kevin on 2019-07-21
 */
@Component("YoutubeDLDownloader")
@Scope("prototype")
class YoutubeDlDownloader(
        itemRepository: ItemRepository,
        podcastRepository: PodcastRepository,
        podcastServerParameters: PodcastServerParameters,
        template: MessagingTemplate,
        mimeTypeService: MimeTypeService,
        val youtubeDl: YoutubeDlService
) : AbstractDownloader(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService) {

    private val log = LoggerFactory.getLogger(YoutubeDlDownloader::class.java)

    override fun download(): Item {
        log.info("Starting download of ${item.url}")
        val url = downloadingItem.urls.first()

        target = getTargetFile(item)

        Try.invoke { youtubeDl.download(url, target!!, DownloadProgressCallback { p, _ ->
            val broadcast = (item.progression ?: 0) < p.roundToInt()
            item.progression = p.roundToInt()
            if (broadcast) {
                convertAndSaveBroadcast()
            }
        }) }.getOrElse { RuntimeException(it) }

        finishDownload()

        return item
    }

    override fun finishDownload() {
        val t = target!!

        val savedPath = Files.walk(t.parent).asSequence()
                .first { it.toAbsolutePath().toString().startsWith(t.toAbsolutePath().toString()) }
                ?: throw RuntimeException("No file found after download with youtube-dl...")

        log.debug("File downloaded by youtube-dl is $savedPath")

        if(savedPath != t) {
            val realExtension = getExtension(savedPath.toString())
            val fileNameWithoutAnyExtension = removeExtension(removeExtension(t.fileName.toString()));

            target = t.resolveSibling("$fileNameWithoutAnyExtension.$realExtension$temporaryExtension")
            Try.invoke { Files.move(savedPath, target) }
        }

        super.finishDownload()
    }

    override fun compatibility(downloadingItem: DownloadingItem): Int {
        if (downloadingItem.urls.size > 1) {
            return Int.MAX_VALUE
        }

        val url = downloadingItem.urls.first().toLowerCase()
        return if ("youtube.com" in url || "www.france.tv" in url) 5
        else if ("www.6play.fr" in url) 5
        else if ("www.tf1.fr" in url) 5
        else if ("www.france.tv" in url) 5
        else Integer.MAX_VALUE
    }

    override fun getFileName(item: Item) = youtubeDl.extractName(item.url!!)
}

@Service
class YoutubeDlService(externalTools: ExternalTools) {

    private val log = LoggerFactory.getLogger(YoutubeDlService::class.java)

    init { YoutubeDL.setExecutablePath(externalTools.youtubedl) }

    fun extractName(url: String): String {
        val request = YoutubeDLRequest(url, null).apply {
            setOption("get-filename")
        }

        return try {
            val name = YoutubeDL.execute(request).out
                    .replace("\n".toRegex(), "")
                    .replace("[^a-zA-Z0-9.-]".toRegex(), "_")

            log.debug("The name of the file fetched from youtube-dl is $name")

            name
        } catch (e: Exception) {
            throw RuntimeException("Error during creation of filename of $url")
        }
    }

    fun download(url: String, destination: Path, callback: DownloadProgressCallback): YoutubeDLResponse {
        val name = destination.fileName.toString()
        val downloadLocation = destination.parent.toAbsolutePath().toString()

        val r = YoutubeDLRequest(url, downloadLocation).apply {
            setOption("retries", 10)
            setOption("output", name)
            setOption("format", "bestvideo[ext=webm]+bestaudio[ext=webm]/best[ext=mp4]+bestaudio[ext=m4a]/best[ext=webm]/best[ext=mp4]")
        }

        return YoutubeDL.execute(r) { progress, etaInSeconds ->
            log.debug("p: {}, s:{}", progress, etaInSeconds)
            callback.onProgressUpdate(progress, etaInSeconds)
        }
    }

}
