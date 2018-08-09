package lan.dk.podcastserver.manager.worker;

import lan.dk.podcastserver.entity.Podcast;
import javax.validation.constraints.NotEmpty;

/**
 * Created by kevin on 22/02/15.
 */
public interface Finder {

    Podcast find(String url);
    Integer compatibility(@NotEmpty String url);
}
