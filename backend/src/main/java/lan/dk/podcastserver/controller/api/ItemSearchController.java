package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import com.github.davinkevin.podcastserver.business.ItemBusiness;
import com.github.davinkevin.podcastserver.business.TagBusiness;
import com.github.davinkevin.podcastserver.entity.Item;
import com.github.davinkevin.podcastserver.entity.Status;
import io.vavr.collection.Set;
import org.slf4j.Logger;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Created by kevin on 23/08/2014.
 * Podcast Server
 */
@RestController
@RequestMapping("/api/items")
public class ItemSearchController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ItemSearchController.class);
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
                             @RequestParam(value = "tags", required = false, defaultValue = "") Set<String> tags,
                             @RequestParam(value = "status", required = false, defaultValue = "") Set<String> _statuses,
                             Pageable pageable) {

        if (!isSearch(q, tags, _statuses)) {
            return itemBusiness.findAll(pageable);
        }

        return itemBusiness.findByTagsAndFullTextTerm(
                q,
                tagBusiness.findAllByName(tags),
                _statuses.flatMap(Status::from),
                pageable
        );
    }

    @GetMapping("reindex")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void reindex() throws InterruptedException {
        itemBusiness.reindex();
    }

    private static Boolean isSearch(String q, Set<String> tags, Set<String> statuses) {
        return !(StringUtils.isEmpty(q) && tags.isEmpty() && statuses.isEmpty());
    }
}
