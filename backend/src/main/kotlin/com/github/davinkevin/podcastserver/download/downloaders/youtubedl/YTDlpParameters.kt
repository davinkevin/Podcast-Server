package com.github.davinkevin.podcastserver.download.downloaders.youtubedl

import org.springframework.boot.context.properties.ConfigurationProperties

// https://github.com/yt-dlp/yt-dlp?tab=readme-ov-file#general-options
@ConfigurationProperties("podcastserver.external-tools.yt-dlp")
data class YTDlpParameters(
    val path: String = "/usr/local/bin/youtube-dl",
    val extraParameters: String = "{}",
    /**
     * Browser TLS fingerprint to impersonate (`--impersonate`) on direct
     * file downloads — Cloudflare-fronted hosts (e.g. private Patreon
     * feeds) reject yt-dlp's default fingerprint with a 403. Requires a
     * yt-dlp build bundling curl_cffi (the official standalone binaries
     * do). Blank disables the option.
     */
    val impersonate: String = "chrome",
)