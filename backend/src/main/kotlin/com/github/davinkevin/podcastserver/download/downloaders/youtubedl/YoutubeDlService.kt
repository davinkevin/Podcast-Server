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
class YoutubeDlService(
    private val youtube: YoutubeDL,
    private val extraParameters: Map<String, String>,
    private val impersonate: String = "chrome",
) {

    private val DEFAULT_FORMAT = "bv+ba"
    private val log = LoggerFactory.getLogger(YoutubeDlService::class.java)

    fun extractName(url: String): String {
        if (!isFromVideoPlatform(url)) {
            return Path(url).fileName.toString()
                .substringBefore("?")
                // HLS manifests are remuxed to mp4 during download, name the result accordingly
                .replace(Regex("\\.m3u8$"), ".mp4")
        }

        val request = YoutubeDLRequest(url).apply {
            setOption("get-filename")
            setOption("merge-output-format", "mp4")

            setOption("format", DEFAULT_FORMAT)
            extraParameters.forEach { setOption(it.key, it.value) }
        }
            .also { log.debug("extract name command: yt-dlp {}", it.buildOptions()) }

        return try {
            val name = youtube.execute(request)
                    .out
                    .replace("\n".toRegex(), "")
                    .replace("[^a-zA-Z0-9.-]".toRegex(), "_")

            log.debug("The name of the file fetched from youtube-dl is $name")

            name
        } catch (e: Exception) {
            log.warn("yt-dlp {}", request.buildOptions())
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
                setOption("format", DEFAULT_FORMAT)
                extraParameters.forEach { setOption(it.key, it.value) }
            } else {
                if (impersonate.isNotBlank()) {
                    // Direct enclosure downloads have to look like a browser:
                    // Cloudflare-fronted hosts (e.g. private Patreon feeds) reject
                    // yt-dlp's default TLS fingerprint with a 403 even though the
                    // URL itself is valid.
                    setOption("impersonate", impersonate)
                }
                if ("m3u8" in url.lowercase()) {
                    // HLS streams come as MPEG-TS segments: remux them into the
                    // mp4 container the rest of the system expects, like the
                    // legacy ffmpeg download path used to do.
                    setOption("remux-video", "mp4")
                }
            }
        }
            .also { log.debug("download command: yt-dlp {}", it.buildOptions()) }

        return youtube.execute(r) { progress ->
            log.debug("progress: {}", progress)
            callback.onProgressUpdate(progress)
        }
    }
}
