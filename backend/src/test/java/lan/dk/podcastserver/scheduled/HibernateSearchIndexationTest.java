package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.business.ItemBusiness;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by kevin on 17/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class HibernateSearchIndexationTest {

    @Mock ItemBusiness itemBusiness;
    @InjectMocks HibernateSearchIndexation hibernateSearchIndexation;

    @Test
    public void should_refresh_index() throws InterruptedException {
        /* When */  hibernateSearchIndexation.refreshIndex();
        /* Then */  verify(itemBusiness, times(1)).reindex();
    }
}
