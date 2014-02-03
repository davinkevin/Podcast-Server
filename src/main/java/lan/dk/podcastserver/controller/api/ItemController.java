package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.utils.facade.PageRequestFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;

/**
 * Created by kevin on 26/12/2013.
 */
@Controller
@RequestMapping("/api/item")
public class ItemController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ItemBusiness itemBusiness;

    @Autowired
    protected ItemDownloadManager itemDownloadManager;

    // RUD (Pas de création car création en cascade depuis les podcast et l'update
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Item findById(@PathVariable int id) {
        return itemBusiness.findOne(id);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PUT, produces = "application/json")
    public Item update(@RequestBody Item item, @PathVariable(value = "id") int id) {
        item.setId(id);
        return itemBusiness.save(item);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable(value = "id") int id) {
        itemBusiness.delete(id);
    }

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Item> findAll() {
        return itemBusiness.findAll();
    }

    @RequestMapping(value="pagination", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Page<Item> findAll(PageRequestFacade pageRequestFacade) {
        logger.debug(pageRequestFacade.toString());
        return itemBusiness.findAll(pageRequestFacade.toPageRequest());
    }

    // Méthode spécifiques :
    @RequestMapping(value="/toDownload", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Item> findAllToDownload() {
        return itemBusiness.findAllToDownload();
    }

    @RequestMapping(value="/toDelete", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Item> findAllToDelete() {
        return itemBusiness.findAllToDelete();
    }

    @RequestMapping(value="{id:[\\d]+}/addtoqueue", method = RequestMethod.GET, produces = "application/json")
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

}
