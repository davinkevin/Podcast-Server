package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import com.gitlab.davinkevin.podcastserver.youtubedl.DownloadProgressCallback
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDL
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDLRequest
import com.gitlab.davinkevin.podcastserver.youtubedl.YoutubeDLResponse
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Created by kevin on 08/05/2020
 */
class YoutubeDlService(private val youtube: YoutubeDL) {

    private val log = LoggerFactory.getLogger(YoutubeDlService::class.java)

    fun extractName(url: String): String {
        if (!isFromVideoPlatform(url)) {
            return Path(url).fileName.toString()
                .substringBefore("?")
        }

        val request = YoutubeDLRequest(url).apply {
            setOption("get-filename")
            setOption("merge-output-format", "mp4")
        }

        return try {
            val name = youtube.execute(request)
                    .out
                    .replace("\n".toRegex(), "")
                    .replace("[^a-zA-Z0-9.-]".toRegex(), "_")

            log.debug("The name of the file fetched from youtube-dl is $name")

            name
        } catch (e: Exception) {
            throw RuntimeException("Error during creation of filename of $url", e)
        }
    }

    fun download(url: String, destination: Path, callback: DownloadProgressCallback): YoutubeDLResponse {
        Files.deleteIfExists(destination)
        val name = destination.fileName.toString()
        val downloadLocation = destination.parent.toAbsolutePath().toString()

        val r = YoutubeDLRequest(url, downloadLocation).apply {
            setOption("retries", 10)
            setOption("output", name)
            setOption("merge-output-format", "mp4")

            if(isFromVideoPlatform(url)) {
                setOption("format", "bv+ba")
            }
        }

        log.debug("yt-dlp {}", r.buildOptions())

        return youtube.execute(r) { progress, etaInSeconds ->
            log.debug("p: {}, s:{}", progress, etaInSeconds)
            callback.onProgressUpdate(progress, etaInSeconds)
        }
    }
}
