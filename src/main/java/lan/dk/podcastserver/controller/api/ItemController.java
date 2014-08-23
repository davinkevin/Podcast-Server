package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.utils.facade.PageRequestFacade;
import lan.dk.podcastserver.utils.facade.SearchItemPageRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Created by kevin on 26/12/2013.
 */
@RestController
@RequestMapping("/api/item")
public class ItemController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource private ItemBusiness itemBusiness;

    @Autowired
    protected ItemDownloadManager itemDownloadManager;

    // RUD (Pas de création car création en cascade depuis les podcast et l'update
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET)
    @ResponseBody
    public Item findById(@PathVariable int id) {
        return itemBusiness.findOne(id);
    }

    @RequestMapping(value="{id:[\\d]+}/podcast", method = RequestMethod.GET)
    @ResponseBody
    public Podcast findPodcastByItemsId(@PathVariable int id) {
        Podcast podcast = itemBusiness.findOne(id).getPodcast();
        podcast.setItems(null);

        return podcast;
    }


    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PUT)
    public Item update(@RequestBody Item item, @PathVariable(value = "id") int id) {
        item.setId(id);
        return itemBusiness.save(item);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable(value = "id") int id) {
        itemBusiness.delete(id);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<Item> findAll() {
        return itemBusiness.findAll();
    }

    @RequestMapping(value="pagination", method = RequestMethod.GET)
    @ResponseBody
    public Page<Item> findAll(PageRequestFacade pageRequestFacade) {
        logger.debug(pageRequestFacade.toString());
        return itemBusiness.findAll(pageRequestFacade.toPageRequest());
    }

    // Méthode spécifiques :
    @RequestMapping(value="/toDownload", method = RequestMethod.GET)
    @ResponseBody
    public List<Item> findAllToDownload() {
        return itemBusiness.findAllToDownload();
    }

    @RequestMapping(value="/toDelete", method = RequestMethod.GET)
    @ResponseBody
    public List<Item> findAllToDelete() {
        return itemBusiness.findAllToDelete();
    }

    @RequestMapping(value="{id:[\\d]+}/addtoqueue", method = RequestMethod.GET)
    @ResponseBody
    public void addToDownloadList(@PathVariable(value = "id") int id) {
        itemDownloadManager.addItemToQueue(id);
    }

    @RequestMapping(value="{id:[\\d]+}/download{ext}", method = RequestMethod.GET)
    public void getEpisodeFile(@PathVariable int id, HttpServletResponse response) {
        try {
            response.sendRedirect(itemBusiness.getEpisodeFile(id));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping(value= {"search/{term}"}, method = RequestMethod.POST )
    @ResponseBody
    public Page<Item> findByDescriptionContaining(@PathVariable("term") String term, @RequestBody SearchItemPageRequestWrapper searchWrapper) {
        SearchItemPageRequestWrapper searchItemPageRequestWrapper = (searchWrapper == null) ? new SearchItemPageRequestWrapper() : searchWrapper;
        return itemBusiness.findByTagsAndFullTextTerm(term, searchItemPageRequestWrapper.getTags(), searchItemPageRequestWrapper.toPageRequest());
    }

    @RequestMapping(value= {"search"}, method = RequestMethod.POST )
    @ResponseBody
    public Page<Item> findByDescriptionContaining(@RequestBody SearchItemPageRequestWrapper searchWrapper) {
        SearchItemPageRequestWrapper searchItemPageRequestWrapper = (searchWrapper == null) ? new SearchItemPageRequestWrapper() : searchWrapper;
        return itemBusiness.findByTagsAndFullTextTerm(null, searchItemPageRequestWrapper.getTags(), searchItemPageRequestWrapper.toPageRequest());
    }


    @RequestMapping(value="reindex", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void reindex() throws InterruptedException {
        itemBusiness.reindex();
    }


}
