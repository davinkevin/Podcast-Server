package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;

/**
 * Created by kevin on 07/06/2014.
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TagBusiness {

    final TagRepository tagRepository;

    public List<Tag> findAll() {
        return tagRepository.findAll();
    }

    public Tag findOne(UUID id) {
        return tagRepository.findOne(id);
    }

    public List<Tag> findByNameLike(String name) {
        return tagRepository.findByNameContainsIgnoreCase(name);
    }

    Set<Tag> getTagListByName(Set<Tag> tagList) {
        return tagList
                .stream()
                .map(t -> findByName(t.getName()))
                .collect(toSet());
    }

    private Tag findByName(String name) {
        return tagRepository.findByNameIgnoreCase(name).orElseGet(() -> tagRepository.save(new Tag().setName(name)));
    }
}
