package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import javaslang.collection.Set;
import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.business.TagBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.utils.facade.PageRequestFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 23/08/2014.
 * Podcast Server
 */
@Slf4j
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemSearchController {

    private final ItemBusiness itemBusiness;
    private final TagBusiness tagBusiness;

    @GetMapping("pagination")
    @JsonView(Item.ItemSearchListView.class)
    public Page<Item> findAll(PageRequestFacade pageRequestFacade) {
        return itemBusiness.findAll(pageRequestFacade.toPageRequest());
    }

    /*@Cacheable("search")*/
    @GetMapping("search")
    @JsonView(Item.ItemSearchListView.class)
    public Page<Item> search(@RequestParam(value = "q", required = false, defaultValue = "") String q,
                             @RequestParam(value = "tags", required = false, defaultValue = "") Set<String> tags,
                             @RequestParam(value = "downloaded", required = false, defaultValue = "") Boolean downloaded,
                             Pageable pageable) {
        return isSearch(q, tags, downloaded)
                ? itemBusiness.findByTagsAndFullTextTerm(q, tagBusiness.findAllByName(tags).toList(), downloaded, pageable)
                : itemBusiness.findAll(pageable);
    }

    @GetMapping("reindex")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void reindex() throws InterruptedException {
        itemBusiness.reindex();
    }

    public static Boolean isSearch(String q, Set<String> tags, Boolean downloaded) {
        return !StringUtils.isEmpty(q) || !tags.isEmpty() || nonNull(downloaded);
    }

}
