package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Cover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CoverRepository extends JpaRepository<Cover, UUID> {}
