package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.repository.TagRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.util.*;

/**
 * Created by kevin on 07/06/2014.
 */
@Component
@Transactional
public class TagBusiness {

    @Resource TagRepository tagRepository;

    public List<Tag> findAll() {
        return tagRepository.findAll();
    }

    public Tag findOne(UUID id) {
        return tagRepository.findOne(id);
    }

    public List<Tag> findByNameLike(String name) {
        return tagRepository.findByNameContainsIgnoreCase(name);
    }

    public Set<Tag> getTagListByName(Set<Tag> tagList) {
        Set<Tag> tagResult = new HashSet<>();

        for(Tag tag : tagList) {
            if (tag.getId() != null) {
                Tag tmpTag = tagRepository.findOne(tag.getId());
                tagResult.add(tmpTag);
            } else {
                tagResult.add(tag);
            }
        }

        return tagResult;
    }
}
