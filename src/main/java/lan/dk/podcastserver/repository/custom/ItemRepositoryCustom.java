package lan.dk.podcastserver.repository.custom;

import java.util.List;

public interface ItemRepositoryCustom {

    void reindex() throws InterruptedException;
    List<Integer> fullTextSearch(String term);

}
