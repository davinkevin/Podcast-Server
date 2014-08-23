package lan.dk.podcastserver.scheduled;

import lan.dk.podcastserver.business.ItemBusiness;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by kevin on 22/08/2014.
 */
@Component
public class HibernateSearchIndexation {

    @Resource ItemBusiness itemBusiness;

    //@Scheduled(fixedDelay = 86400000, initialDelay = 10000)
    private void refreshIndex() throws InterruptedException {
        itemBusiness.reindex();
    }

}
