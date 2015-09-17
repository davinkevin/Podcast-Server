package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.service.WorkerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 14/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class TypeControllerTest {

    @Mock WorkerService workerService;
    @InjectMocks TypeController typeController;

    @Test
    public void should_return_all_abstract_type() throws NoSuchMethodException {
        /* Given */
        Set<AbstractUpdater.Type> types = new HashSet<>();
        when(workerService.types()).thenReturn(types);

        /* When */
        Set<AbstractUpdater.Type> returnTypes = typeController.types();

        /* Then */
        assertThat(returnTypes).isSameAs(types);
    }

}