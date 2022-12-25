package com.gitlab.davinkevin.podcastserver.youtubedl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class YoutubeDLTest {

    private final static String DIRECTORY = System.getProperty("java.io.tmpdir");
    private final static String VIDEO_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
    private final static String NONE_EXISTENT_VIDEO_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcZ";

    public static final YoutubeDL YOUTUBE_DL = new YoutubeDL();

    /**
     * @ Test public void testUsingOwnExecutablePath() throws YoutubeDLException
     * { YoutubeDL.setExecutablePath("/usr/bin/youtube-dl");
     * assertNotNull(YoutubeDL.getVersion());
    }*
     */
    @Test
    public void testGetVersion() throws YoutubeDLException {
        assertNotNull(YOUTUBE_DL.getVersion());
    }

    @Test
    public void testElapsedTime() throws YoutubeDLException {

        var startTime = System.nanoTime();
        var request = new YoutubeDLRequest();
        
        request.setOption("version");
        var response = YOUTUBE_DL.execute(request);

        var elapsedTime = (int) (System.nanoTime() - startTime);

        assertTrue(elapsedTime > response.getElapsedTime());
    }


    @Test
    public void testSimulateDownload() throws YoutubeDLException {
        var request = new YoutubeDLRequest();
        request.setUrl(VIDEO_URL);
        request.setOption("simulate");

        var response = YOUTUBE_DL.execute(request);

        assertEquals("youtube-dl " + VIDEO_URL + " --simulate", response.getCommand());
    }

    @Test
    public void testDirectory() throws YoutubeDLException {

        var request = new YoutubeDLRequest(VIDEO_URL, DIRECTORY);
        request.setOption("simulate");

        var response = YOUTUBE_DL.execute(request);

        assertEquals(DIRECTORY, response.getDirectory());
    }

    @Test
    public void testGetVideoInfo() throws YoutubeDLException {
        var videoInfo = YOUTUBE_DL.getVideoInfo(VIDEO_URL);
        assertNotNull(videoInfo);
    }

    @Test
    public void testGetFormats() throws YoutubeDLException {
        var formats = YOUTUBE_DL.getFormats(VIDEO_URL);
        assertNotNull(formats);
        assertTrue(formats.size() > 0);
    }

    @Test
    public void testGetThumbnails() throws YoutubeDLException {
        var thumbnails = YOUTUBE_DL.getThumbnails(VIDEO_URL);
        assertNotNull(thumbnails);
        assertTrue(thumbnails.size() > 0);
    }

    @Test
    public void testGetTags() throws YoutubeDLException {
        var tags = YOUTUBE_DL.getTags(VIDEO_URL);
        assertNotNull(tags);
        assertTrue(tags.size() > 0);
    }

    @Test
    public void testGetCategories() throws YoutubeDLException {
        List<String> categories = YOUTUBE_DL.getCategories(VIDEO_URL);
        assertNotNull(categories);
        assertTrue(categories.size() > 0);
    }

    @Test
    public void testFailGetNonExistentVideoInfo() throws YoutubeDLException {
        assertThrows(YoutubeDLException.class, () -> {
            YOUTUBE_DL.getVideoInfo(NONE_EXISTENT_VIDEO_URL);
        });
    }
}
