package lan.dk.podcastserver.manager.worker.selector;

import lan.dk.podcastserver.manager.worker.extractor.Extractor;
import lan.dk.podcastserver.manager.worker.extractor.NoOpExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.TargetClassAware;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;

/**
 * Created by kevin on 03/12/2017
 */
@Service
@RequiredArgsConstructor
public class ExtractorSelector {

    static final Extractor NO_OP_EXTRACTOR = new NoOpExtractor();

    private final ApplicationContext applicationContext;
    private final Set<Extractor> extractors;

    public Extractor of(String url) {
        if (StringUtils.isEmpty(url)) {
            return NO_OP_EXTRACTOR;
        }

        return extractors
                .stream()
                .min(Comparator.comparing(e -> e.compatibility(url)))
                .map(d -> TargetClassAware.class.isInstance(d) ? TargetClassAware.class.cast(d).getTargetClass() : d.getClass())
                .map(applicationContext::getBean)
                .map(Extractor.class::cast)
                .orElse(NO_OP_EXTRACTOR);
    }

}
