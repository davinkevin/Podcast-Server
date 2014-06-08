package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.Custom.ItemRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ItemRepository extends JpaRepository<Item, Integer>, ItemRepositoryCustom, JpaSpecificationExecutor {

    List<Item> findByStatus(@Param(value="status") String status);
}
