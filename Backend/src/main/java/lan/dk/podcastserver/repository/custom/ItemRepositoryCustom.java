package lan.dk.podcastserver.repository.custom;

import javaslang.collection.List;

import java.util.UUID;

public interface ItemRepositoryCustom {

    void reindex() throws InterruptedException;
    List<UUID> fullTextSearch(String term);

}
