package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.utils.facade.PageRequestFacade;
import lan.dk.podcastserver.utils.facade.SearchItemPageRequestWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ItemSearchController {

    final private ItemBusiness itemBusiness;

    @RequestMapping(value="pagination", method = RequestMethod.GET)
    @JsonView(Item.ItemSearchListView.class)
    public Page<Item> findAll(PageRequestFacade pageRequestFacade) {
        return itemBusiness.findAll(pageRequestFacade.toPageRequest());
    }

    @RequestMapping(value= "search", method = RequestMethod.POST )
    @JsonView(Item.ItemSearchListView.class)
    public Page<Item> search(@RequestBody SearchItemPageRequestWrapper searchWrapper) {
        return searchWrapper.isSearch()
                ? itemBusiness.findByTagsAndFullTextTerm(searchWrapper.getTerm(), searchWrapper.getTags(), searchWrapper.getDownloaded(), searchWrapper.toPageRequest())
                : itemBusiness.findAll(searchWrapper.toPageRequest());
    }

    @RequestMapping(value="reindex", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void reindex() throws InterruptedException {
        itemBusiness.reindex();
    }


}
