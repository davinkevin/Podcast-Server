package lan.dk.podcastserver.business;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Created by kevin on 07/06/2014.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TagBusiness {

    private final TagRepository tagRepository;

    public List<Tag> findAll() {
        return List.ofAll(tagRepository.findAll());
    }

    public Tag findOne(UUID id) {
        return tagRepository.findById(id).orElseThrow(() -> new Error("Tag with ID "+ id +" not found"));
    }

    public Set<Tag> findByNameLike(String name) {
        return tagRepository.findByNameContainsIgnoreCase(name);
    }

    Set<Tag> getTagListByName(Set<Tag> tagList) {
        return tagList.map(t -> findByName(t.getName()));
    }

    private Tag findByName(String name) {
        return tagRepository
                .findByNameIgnoreCase(name)
                .getOrElse(() -> tagRepository.save(Tag.builder().name(name).build()));
    }

    public Set<Tag> findAllByName(Set<String> names){
        return names.flatMap(tagRepository::findByNameIgnoreCase);
    }
}
