package lan.dk.podcastserver.controller.api;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.Type;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 14/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class TypeControllerTest {

    private @Mock UpdaterSelector updaterSelector;
    private @InjectMocks TypeController typeController;

    @Test
    public void should_return_all_abstract_type() throws NoSuchMethodException {
        /* Given */
        Set<Type> types = HashSet.empty();
        when(updaterSelector.types()).thenReturn(types);

        /* When */
        io.vavr.collection.Set<Type> returnTypes = typeController.types();

        /* Then */
        assertThat(returnTypes).isSameAs(types);
    }

}
