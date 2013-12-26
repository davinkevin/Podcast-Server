package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by kevin on 26/12/2013.
 */
@Component
@Transactional
public class ItemBusiness {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private @Resource ItemRepository itemRepository;
    protected @Resource ItemDownloadManager itemDownloadManager;
    private @Value("${numberofdaytodownload}") int numberOfDayToDownload;

    //** Delegation Repository **//
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public List<Item> findAll(Sort sort) {
        return itemRepository.findAll(sort);
    }

    public Page<Item> findAll(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    public Item save(Item entity) {
        return itemRepository.save(entity);
    }

    public Item findOne(Integer integer) {
        return itemRepository.findOne(integer);
    }

    public void delete(Integer id) {
        Item itemToDelete = itemRepository.findOne(id);

        //* Si le téléchargement est en cours ou en attente : *//
        itemDownloadManager.removeItemFromQueueAndDownload(itemToDelete);

        itemToDelete.getPodcast().getItems().remove(itemToDelete);
        this.delete(itemToDelete);
    }

    public void delete(Item entity) {
        itemRepository.delete(entity);
    }

    public List<Item> findAllItemNotDownloadedNewerThan(Date date) {
        return itemRepository.findAllItemNotDownloadedNewerThan(date);
    }

    public List<Item> findAllItemDownloadedOlderThan(Date date) {
        return itemRepository.findAllItemDownloadedOlderThan(date);
    }

    public List<Item> findByStatus(String status) {
        return itemRepository.findByStatus(status);
    }

    //****************************//

    public List<Item> findAllToDownload() {
        Calendar c = Calendar.getInstance();
        c.setTime(c.getTime());
        c.add(Calendar.DATE, numberOfDayToDownload*-1);  // number of days to add
        return this.findAllItemNotDownloadedNewerThan(c.getTime());
    }

    public List<Item> findAllToDelete() {
        Calendar c = Calendar.getInstance();
        c.setTime(c.getTime());
        c.add(Calendar.DATE, numberOfDayToDownload*-1);  // number of days to add
        return this.findAllItemDownloadedOlderThan(c.getTime());
    }

    public String getEpisodeFile(int id) {
        Item item = this.findOne(id);
        try {
            if (item.getLocalUrl() != null) {
                logger.debug("Interne - Item " + id + " : " + item.getLocalUrl());
                URL redirectionURL = new URL(item.getLocalUrl());
                URI redirectionURI = new URI(redirectionURL.getProtocol(), null, redirectionURL.getHost(), redirectionURL.getPort(), redirectionURL.getPath(), null, null);
                return redirectionURI.toASCIIString();
            }
            else {
                logger.debug("Externe - Item " + id + " : " + item.getUrl());
                return item.getUrl();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return "";
    }



}
