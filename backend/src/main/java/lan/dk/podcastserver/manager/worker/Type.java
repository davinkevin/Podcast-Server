package lan.dk.podcastserver.manager.worker;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by kevin on 25/12/2017
 */
public class Type {
    private final String key;
    private final String name;

    public Type(String key, String name) {
        this.key = key;
        this.name = name;
    }

    @JsonProperty("key")
    public String key() {
        return key;
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }
}
