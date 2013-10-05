package lan.dk.podcastserver.service.api;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
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

@Controller
@RequestMapping("/api/item")
public class ItemService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private ItemRepository itemRepository;

    @Autowired
    protected ItemDownloadManager itemDownloadManager;

    @Value("${numberofdaytodownload}")
    private int numberOfDayToDownload;

    // RUD (Pas de création car création en cascade depuis les podcast et l'update
    @Transactional(readOnly = true)
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Item findById(@PathVariable int id) {
        return itemRepository.findOne(id);
    }

    @Transactional//(rollbackFor = PersonNotFoundException.class)
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.PUT, produces = "application/json")
    public Item update(@RequestBody Item item, @PathVariable(value = "id") int id) {
        item.setId(id);
        return itemRepository.save(item);
    }

    public Item update(Item item) {
        return itemRepository.save(item);
    }

    @Transactional//(rollbackFor = PersonNotFoundException.class)
    @RequestMapping(value="{id:[\\d]+}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void delete (@PathVariable(value = "id") int id) {
        Item itemToDelete = itemRepository.findOne(id);
        logger.debug("Delete of Item : " + itemToDelete.toString());

        //* Si le téléchargement est en cours ou en attente : *//
        if (itemDownloadManager.getDownloadingQueue().containsKey(itemToDelete)) {
            itemDownloadManager.stopDownload(itemToDelete.getId());
        } else if (itemDownloadManager.getWaitingQueue().contains(itemToDelete)) {
            itemDownloadManager.removeItemFromQueue(itemToDelete);
        }
        itemToDelete.getPodcast().getItems().remove(itemToDelete);
        itemRepository.delete(itemToDelete);

    }

    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Transactional(readOnly = true)
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @RequestMapping(value="pagination", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    @Transactional(readOnly = true)
    public Page<Item> findAll(PageRequestFacade pageRequestFacade) {
        logger.debug(pageRequestFacade.toString());
        return itemRepository.findAll(pageRequestFacade.toPageRequest());
    }

    // Méthode spécifiques :
    @Transactional(readOnly = true)
    @RequestMapping(value="/toDownload", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Item> findAllToDownload() {

        Calendar c = Calendar.getInstance();
        c.setTime(c.getTime());
        c.add(Calendar.DATE, numberOfDayToDownload*-1);  // number of days to add


        return itemRepository.findAllItemNotDownloadedNewerThan(c.getTime());
    }

    @Transactional(readOnly = true)
    @RequestMapping(value="/toDelete", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public List<Item> findAllToDelete() {

        Calendar c = Calendar.getInstance();
        c.setTime(c.getTime());
        c.add(Calendar.DATE, numberOfDayToDownload*-1);  // number of days to add


        return itemRepository.findAllItemDownloadedOlderThan(c.getTime());
    }

    @Transactional(readOnly = true)
    @RequestMapping(value="{id:[\\d]+}/download", method = RequestMethod.GET)
    public void getEpisodeFile(@PathVariable int id, HttpServletResponse response) {
        Item item = itemRepository.findOne(id);
            try {
                if (item.getLocalUrl() != null) {
                    logger.debug("Interne - Item " + id + " : " + item.getLocalUrl());
                    URL redirectionURL = new URL(item.getLocalUrl());
                    URI redirectionURI = new URI(redirectionURL.getProtocol(), null, redirectionURL.getHost(), redirectionURL.getPort(), redirectionURL.getPath(), null, null);
                    response.sendRedirect(redirectionURI.toASCIIString());

                }
                else {
                    logger.debug("Externe - Item " + id + " : " + item.getUrl());
                    response.sendRedirect(item.getUrl());
                }
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (URISyntaxException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
    }


}
