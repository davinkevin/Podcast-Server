package lan.dk.podcastserver.manager.worker.updater;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;

/**
 * Created by kevin on 25/12/2017
 */
@RequiredArgsConstructor
public class Type {
    private final String key;
    private final String name;

    @JsonProperty("key")
    public String key() {
        return key;
    }

    @JsonProperty("name")
    public String name() {
        return name;
    }
}
