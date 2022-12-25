package com.gitlab.davinkevin.podcastserver.youtubedl;

import java.util.*;

/**
 * YoutubeDL request
 */
public class YoutubeDLRequest {

    /**
     * Executable working directory
     */
    private String directory;

    /**
     * Video Url
     */
    private String url;

    /**
     * List of executable options
     */
    private Map<String, String> options = new HashMap<String, String>();

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getOption() {
        return options;
    }

    public void setOption(String key) {
        options.put(key, "");
    }

    public void setOption(String key, String value) {
        if (value == null) {
            this.setOption(key);
            return;
        }
        options.put(key, value);
    }

    public void setOption(String key, int value) {
        options.put(key, String.valueOf(value));
    }

    /**
     * Constructor
     */
    public YoutubeDLRequest() {

    }

    /**
     * Construct a request with a videoUrl
     *
     * @param url
     */
    public YoutubeDLRequest(String url) {
        this.url = url;
    }

    /**
     * Construct a request with a videoUrl and working directory
     *
     * @param url
     * @param directory
     */
    public YoutubeDLRequest(String url, String directory) {
        this.url = url;
        this.directory = directory;
    }

    private String formatOption(String key, String value) {
        return String
                .format("--%s %s", key, value)
                .trim();
    }

    private String formatOption(Map.Entry<String, String> option) {
        return this.formatOption(option.getKey(), option.getValue());
    }

    private String concatFormattedOptions(String concated, String next) {
        return concated.concat(" ").concat(next);
    }

    /**
     * Transform options to a string that the executable will execute
     *
     * @return Command string
     */
    public String buildOptions() {

        final StringBuilder builder = new StringBuilder();

        if (this.url != null) {
            builder.append(this.url).append(" ");
        }

        if (this.options.isEmpty()) {
            return builder.toString().trim();
        }

        final String optionsString = this.options.entrySet().stream()
                .map(this::formatOption)
                .reduce(this::concatFormattedOptions)
                .get();
        
        return builder
                .append(optionsString)
                .toString();
    }
}
