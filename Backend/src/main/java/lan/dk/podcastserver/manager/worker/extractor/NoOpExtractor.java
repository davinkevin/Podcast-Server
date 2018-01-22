package lan.dk.podcastserver.manager.worker.extractor;

import io.vavr.Tuple2;
import lan.dk.podcastserver.entity.Item;
import lombok.extern.slf4j.Slf4j;

import static io.vavr.API.Tuple;

/**
 * Created by kevin on 03/12/2017
 */
@Slf4j
public class NoOpExtractor implements Extractor {
    @Override
    public Tuple2<Item, String> extract(Item item) {
        return Tuple(item, item.getUrl());
    }

    @Override
    public Integer compatibility(String url) {
        return Integer.MAX_VALUE;
    }
}
