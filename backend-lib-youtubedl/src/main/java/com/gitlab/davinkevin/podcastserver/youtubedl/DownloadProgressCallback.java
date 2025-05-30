package com.gitlab.davinkevin.podcastserver.youtubedl;

public interface DownloadProgressCallback {
    void onProgressUpdate(float progress);
}
