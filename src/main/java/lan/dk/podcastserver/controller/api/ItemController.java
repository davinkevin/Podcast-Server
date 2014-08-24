package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/api/podcast/{idPodcast}/items")
public class ItemController {

    @Resource private ItemBusiness itemBusiness;

    @Autowired
    protected ItemDownloadManager itemDownloadManager;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<Item> findAll(@PathVariable Integer idPodcast) {
        return itemBusiness.findByPodcast(idPodcast);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET)
    @ResponseBody
    public Item findById(@PathVariable int id) {
        return itemBusiness.findOne(id);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PUT)
    @ResponseBody
    public Item update(@RequestBody Item item, @PathVariable(value = "id") int id) {
        item.setId(id);
        return itemBusiness.save(item);
    }

    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable(value = "id") int id) {
        itemBusiness.delete(id);
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

    @RequestMapping(value = "{id:[\\d]+}/reset", method = RequestMethod.GET)
    @ResponseBody
    public Item reset(@PathVariable Integer id) {
        return itemBusiness.reset(id);
    }
}
