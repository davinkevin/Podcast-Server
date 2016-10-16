package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.utils.facade.PageRequestFacade;
import lan.dk.podcastserver.utils.facade.SearchItemPageRequestWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Created by kevin on 23/08/2014.
 * Podcast Server
 */
@Slf4j
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemSearchController {

    final ItemBusiness itemBusiness;

    @GetMapping("pagination")
    @JsonView(Item.ItemSearchListView.class)
    public Page<Item> findAll(PageRequestFacade pageRequestFacade) {
        return itemBusiness.findAll(pageRequestFacade.toPageRequest());
    }

    @Cacheable("search")
    @PostMapping("search")
    @JsonView(Item.ItemSearchListView.class)
    public Page<Item> search(@RequestBody SearchItemPageRequestWrapper searchWrapper) {
        return searchWrapper.isSearch()
                ? itemBusiness.findByTagsAndFullTextTerm(searchWrapper.getTerm(), searchWrapper.getTags(), searchWrapper.getDownloaded(), searchWrapper.toPageRequest())
                : itemBusiness.findAll(searchWrapper.toPageRequest());
    }

    @GetMapping("reindex")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void reindex() throws InterruptedException {
        itemBusiness.reindex();
    }


}
