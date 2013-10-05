package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.Custom.ItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ItemRepository extends JpaRepository<Item, Integer>, ItemRepositoryCustom {



}
