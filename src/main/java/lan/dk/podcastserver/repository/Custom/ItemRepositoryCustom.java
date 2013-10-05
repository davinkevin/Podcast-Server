package lan.dk.podcastserver.repository.Custom;

import lan.dk.podcastserver.entity.Item;

import java.util.Date;
import java.util.List;

public interface ItemRepositoryCustom {

    List<Item> findAllItemNotDownloadedNewerThan(Date date);
    List<Item> findAllItemDownloadedOlderThan(Date date);

}
