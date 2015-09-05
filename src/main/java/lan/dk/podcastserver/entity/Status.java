package lan.dk.podcastserver.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Created by kevin on 18/02/15.
 */
public enum Status {
    NOT_DOWNLOADED,
    DELETED,
    STARTED,
    FINISH,
    STOPPED,
    PAUSED;

    @JsonCreator
    public static Status of(String value) {
        return Status.valueOf(value);
    }
    
}
