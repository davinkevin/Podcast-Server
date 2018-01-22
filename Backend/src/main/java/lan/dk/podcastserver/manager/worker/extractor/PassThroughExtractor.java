package lan.dk.podcastserver.manager.worker.extractor;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import lan.dk.podcastserver.entity.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static java.lang.Integer.MAX_VALUE;
import static io.vavr.API.Tuple;

/**
 * Created by kevin on 03/12/2017
 */
@Slf4j
@Component
@Scope("prototype")
public class PassThroughExtractor implements Extractor {
    
    @Override
    public Tuple2<Item, String> extract(Item item) {
        return Tuple(item, item.getUrl());
    }

    @Override
    public Integer compatibility(String url) {
        return MAX_VALUE-1;
    }

}
