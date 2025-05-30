package com.gitlab.davinkevin.podcastserver.youtubedl.utils;

import com.gitlab.davinkevin.podcastserver.youtubedl.DownloadProgressCallback;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StreamProcessExtractor extends Thread {
    private static final String GROUP_PERCENT = "percent";
    private static final String GROUP_MINUTES = "minutes";
    private static final String GROUP_SECONDS = "seconds";
    private final InputStream stream;
    private final StringBuffer buffer;
    private final DownloadProgressCallback callback;

    private final Pattern p = Pattern.compile("\\[download]\\s+(?<percent>\\d+\\.\\d+)%.*");

    public static final String[] progressParameter = {"--progress-template", "download:[download] %(progress._percent)s%", "--newline"};

    public StreamProcessExtractor(StringBuffer buffer, InputStream stream, DownloadProgressCallback callback) {
        this.stream = stream;
        this.buffer = buffer;
        this.callback = callback;
        this.start();
    }

    public void run() {
        try {
            StringBuilder currentLine = new StringBuilder();
            int nextChar;
            while ((nextChar = stream.read()) != -1) {
                buffer.append((char) nextChar);
                if ((nextChar == '\r' || nextChar == '\n') && callback != null) {
                    processOutputLine(currentLine.toString());
                    currentLine.setLength(0);
                    continue;
                }
                currentLine.append((char) nextChar);
            }
        } catch (IOException ignored) {
        }
    }

    private void processOutputLine(String line) {
        Matcher m = p.matcher(line);
        if (m.matches()) {
            float progress = Float.parseFloat(m.group(GROUP_PERCENT));
            callback.onProgressUpdate(progress);
        }
    }

}
