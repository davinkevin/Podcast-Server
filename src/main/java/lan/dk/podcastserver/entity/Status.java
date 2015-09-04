package lan.dk.podcastserver.entity;

import java.util.Arrays;

/**
 * Created by kevin on 18/02/15.
 */
public enum Status {
    NOT_DOWNLOADED("Not Downloaded"),
    DELETED("Deleted"),
    STARTED("Started"),
    FINISH("Finish"),
    STOPPED("Stopped"), 
    PAUSED("Paused");

    private final String name;

    Status(String s) {
        name = s;
    }

    public boolean is(String otherName){
        return (otherName != null) && name.equals(otherName);
    }

    public String value() {
        return name;
    }
    
    public static Status byValue(String name) {
        return Arrays.stream(Status.values())
                .filter(status -> status.is(name))
                .findFirst()
                .orElse(null);
    }
    
}
