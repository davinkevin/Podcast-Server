package lan.dk.podcastserver.manager.selector;

import io.vavr.collection.HashSet;
import lan.dk.podcastserver.manager.worker.Extractor;
import lan.dk.podcastserver.manager.worker.noop.PassThroughExtractor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Set;

import static lan.dk.podcastserver.manager.selector.ExtractorSelector.NO_OP_EXTRACTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 03/12/2017
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ExtractorSelectorTest {

    private ExtractorSelector selector;

    private @Mock PassThroughExtractor passThroughExtractor;
    private @Mock ApplicationContext applicationContext;
    private Set<Extractor> extractors;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(passThroughExtractor.compatibility(anyString())).thenCallRealMethod();
        when(applicationContext.getBean(any(Class.class))).then(findBean());

        extractors = HashSet.<Extractor>of(passThroughExtractor).toJavaSet();
        selector = new ExtractorSelector(applicationContext, extractors);
    }

    private Answer<Extractor> findBean() {
        return i -> extractors
                .stream()
                .filter(d -> i.getArgumentAt(0, Class.class) == d.getClass())
                .findFirst()
                .orElse(NO_OP_EXTRACTOR);
    }

    @Test
    public void should_return_no_op_if_empty_string() {
        /* When  */ Extractor extractorClass = selector.of("");
        /* Then  */ assertThat(extractorClass).isEqualTo(ExtractorSelector.NO_OP_EXTRACTOR);
    }

    @Test
    public void should_do_pass_through_for_now() {
        /* When  */ Extractor extractorClass = selector.of("http://www.podtrac.com/pts/redirect.mp3/twit.cachefly.net/audio/tnt/tnt1217/tnt1217.mp3");
        /* Then  */ assertThat(extractorClass).isEqualTo(passThroughExtractor);
    }

}