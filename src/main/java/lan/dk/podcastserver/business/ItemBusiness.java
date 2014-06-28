package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static lan.dk.podcastserver.repository.Specification.ItemSpecifications.isInTags;

/**
 * Created by kevin on 26/12/2013.
 */
@Component
@Transactional
public class ItemBusiness {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource ItemRepository itemRepository;
    protected @Resource ItemDownloadManager itemDownloadManager;
    @Value("${numberofdaytodownload:3}") int numberOfDayToDownload;

    //** Delegation Repository **//
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Item> findAll(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Item> findAllByPodcastTags(List<Tag> tags, Pageable pageable) {
        return itemRepository.findAll(isInTags(tags.toArray(new Tag[tags.size()])), pageable);
    }

    public List<Item> save(Iterable<Item> entities) {
        return itemRepository.save(entities);
    }

    public Item save(Item entity) {
        return itemRepository.save(entity);
    }

    @Transactional(readOnly = true)
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

    public void deleteAll() {
        itemRepository.deleteAll();
    }

    @Transactional(readOnly = true)
    public List<Item> findAllItemNotDownloadedNewerThan(Date date) {
        return itemRepository.findAllItemNotDownloadedNewerThan(date);
    }

    @Transactional(readOnly = true)
    public List<Item> findAllItemDownloadedOlderThan(Date date) {
        return itemRepository.findAllItemDownloadedOlderThan(date);
    }

    @Transactional(readOnly = true)
    public List<Item> findByStatus(String status) {
        return itemRepository.findByStatus(status);
    }

    //****************************//

    @Transactional(readOnly = true)
    public List<Item> findAllToDownload() {
        Calendar c = Calendar.getInstance();
        c.setTime(c.getTime());
        c.add(Calendar.DATE, numberOfDayToDownload*-1);  // number of days to add
        return this.findAllItemNotDownloadedNewerThan(c.getTime());
    }

    @Transactional(readOnly = true)
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
                logger.info("Interne - Item " + id + " : " + item.getLocalUrl());
                URL redirectionURL = new URL(item.getLocalUrl());
                URI redirectionURI = new URI(redirectionURL.getProtocol(), null, redirectionURL.getHost(), redirectionURL.getPort(), redirectionURL.getPath(), null, null);
                return redirectionURI.toASCIIString();
            }
            else {
                logger.info("Externe - Item " + id + " : " + item.getUrl());
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
