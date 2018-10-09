package com.github.davinkevin.podcastserver.service.factory

import com.github.axet.vget.VGet
import com.github.axet.vget.VGet.parser
import com.github.axet.vget.info.VideoInfo
import com.github.axet.wget.WGet
import com.github.axet.wget.info.DownloadInfo
import org.springframework.stereotype.Service
import java.io.File
import java.net.MalformedURLException
import java.net.URL

/**
 * Created by kevin on 15/09/15 for Podcast Server
 */
@Service
class WGetFactory {

    @Throws(MalformedURLException::class)
    fun parser(url: String) = parser(url.toUrl())!!

    fun newVGet(videoInfo: VideoInfo) = VGet(videoInfo, null)

    fun newWGet(info: DownloadInfo, targetFile: File) = WGet(info, targetFile)

    fun newDownloadInfo(url: String) = DownloadInfo(url.toUrl())

}

private fun String.toUrl() = URL(this)
