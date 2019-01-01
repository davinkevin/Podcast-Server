package lan.dk.podcastserver.controller.api;

import com.github.davinkevin.podcastserver.manager.selector.UpdaterSelector;
import com.github.davinkevin.podcastserver.manager.worker.Type;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 14/09/15 for Podcast Server
 */
@ExtendWith(MockitoExtension.class)
public class TypeControllerTest {

    private @Mock UpdaterSelector updaterSelector;
    private @InjectMocks TypeController typeController;

    @Test
    public void should_return_all_abstract_type() {
        /* Given */
        Set<Type> types = HashSet.empty();
        when(updaterSelector.types()).thenReturn(types);

        /* When */
        java.util.Set<Type> returnTypes = typeController.types();

        /* Then */
        assertThat(returnTypes).isEqualTo(types.toJavaSet());
    }

}
