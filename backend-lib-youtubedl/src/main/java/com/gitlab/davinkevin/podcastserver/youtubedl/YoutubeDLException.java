package com.gitlab.davinkevin.podcastserver.youtubedl;

/**
 * YoutubeDL Exception
 */
public class YoutubeDLException extends Exception {

    /**
     * Exception message
     */
    private String message;

    /**
     * Construct YoutubeDLException with a message
     * @param message
     */
    public YoutubeDLException(String message) {
        this.message = message;
    }

    /**
     * Construct YoutubeDLException from another exception
     * @param e Any exception
     */
    public YoutubeDLException(Exception e) {
        message = e.getMessage();
    }

    /**
     * Get exception message
     * @return exception message
     */
    @Override
    public String getMessage() {
        return message;
    }
}
