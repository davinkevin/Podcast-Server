package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.business.ItemBusiness;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by kevin on 22/08/2014.
 */
@Component
@RequiredArgsConstructor
public class HibernateSearchIndexation {

    private final ItemBusiness itemBusiness;

    @Scheduled(fixedDelay = 86400000)
    public void refreshIndex() throws InterruptedException {
        itemBusiness.reindex();
    }

}
