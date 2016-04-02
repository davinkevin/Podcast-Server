package lan.dk.podcastserver.repository.custom;

import java.util.List;
import java.util.UUID;

public interface ItemRepositoryCustom {

    void reindex() throws InterruptedException;
    List<UUID> fullTextSearch(String term);

}
