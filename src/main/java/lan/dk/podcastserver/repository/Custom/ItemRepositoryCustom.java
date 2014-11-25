package lan.dk.podcastserver.repository.Custom;

import java.util.List;

public interface ItemRepositoryCustom {

    void reindex() throws InterruptedException;
    List<Integer> fullTextSearch(String term);

}
