package lan.dk.podcastserver.manager.worker.extractor;

import io.vavr.Tuple2;
import lan.dk.podcastserver.entity.Item;

/**
 * Created by kevin on 03/12/2017
 */
public interface Extractor {

    Tuple2<Item, String> extract(Item item);

    Integer compatibility(String url);
}
