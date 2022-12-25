package com.gitlab.davinkevin.podcastserver.youtubedl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class YoutubeDLRequestTest {

    @Test
    public void testBuildOptionStandalone() {

        YoutubeDLRequest request = new YoutubeDLRequest();
        request.setOption("help");

        assertEquals("--help", request.buildOptions());
    }

    @Test
    public void testBuildOptionWithValue() {

        YoutubeDLRequest request = new YoutubeDLRequest();
        request.setOption("password", "1234");

        assertEquals("--password 1234", request.buildOptions());
    }

    @Test
    public void testBuildChainOptionWithValue() {

        YoutubeDLRequest request = new YoutubeDLRequest();
        request.setOption("password", "1234");
        request.setOption("username", "1234");

        assertEquals("--password 1234 --username 1234", request.buildOptions());
    }
}
