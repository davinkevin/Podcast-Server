package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.davinkevin.podcastserver.business.ItemBusiness;
import com.github.davinkevin.podcastserver.business.TagBusiness;
import com.github.davinkevin.podcastserver.entity.Item;
import com.github.davinkevin.podcastserver.entity.Status;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Created by kevin on 23/08/2014.
 * Podcast Server
 */
@RestController
@RequestMapping("/api/items")
public class ItemSearchController {

    private static final Logger log = LoggerFactory.getLogger(ItemSearchController.class);
    private final ItemBusiness itemBusiness;
    private final TagBusiness tagBusiness;

    @java.beans.ConstructorProperties({"itemBusiness", "tagBusiness"})
    public ItemSearchController(ItemBusiness itemBusiness, TagBusiness tagBusiness) {
        this.itemBusiness = itemBusiness;
        this.tagBusiness = tagBusiness;
    }

    @Cacheable("search")
    @GetMapping("search")
    @JsonView(Item.ItemSearchListView.class)
    public Page<Item> search(@RequestParam(value = "q", required = false, defaultValue = "") String q,
                             @RequestParam(value = "tags", required = false, defaultValue = "") String _tags,
                             @RequestParam(value = "status", required = false, defaultValue = "") String _statuses,

                             @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                             @RequestParam(value = "size", required = false, defaultValue = "12") Integer size,
                             @RequestParam(value = "sort", required = false, defaultValue = "pubDate,DESC") String sort
    ) {
        String field = StringUtils.substringBefore(sort, ",");
        Sort.Direction direction = Sort.Direction.fromString(StringUtils.substringAfter(sort, ","));
        PageRequest pageable = PageRequest.of(page, size, new Sort(direction, field));

        Set<String> tags = StringUtils.isEmpty(_tags) ? HashSet.empty(): HashSet.of(_tags.split(","));
        Set<String> statuses = StringUtils.isEmpty(_statuses) ? HashSet.empty(): HashSet.of(_statuses.split(","));

        if (!isSearch(q, tags, statuses)) {
            return itemBusiness.findAll(pageable);
        }

        return itemBusiness.findByTagsAndFullTextTerm(
                q,
                tagBusiness.findAllByName(tags),
                statuses.flatMap(Status::from),
                pageable
        );
    }

    @GetMapping("reindex")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void reindex() throws InterruptedException {
        itemBusiness.reindex();
    }

    private static Boolean isSearch(String q, Set<String> tags, Set<String> statuses) {
        return !StringUtils.isEmpty(q) || !tags.isEmpty() || !statuses.isEmpty();
    }
}
