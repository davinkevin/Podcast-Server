package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specifications;
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

import static lan.dk.podcastserver.repository.Specification.ItemSpecifications.hasId;
import static lan.dk.podcastserver.repository.Specification.ItemSpecifications.isInTags;
import static org.springframework.data.jpa.domain.Specifications.where;

/**
 * Created by kevin on 26/12/2013.
 */
@Component
@Transactional
public class ItemBusiness {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource ItemDownloadManager itemDownloadManager;

    @Resource ItemRepository itemRepository;
    @Resource PodcastBusiness podcastBusiness;

    @Value("${numberofdaytodownload:3}") int numberOfDayToDownload;

    //** Delegation Repository **//
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Item> findAll(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Page<Item> findAllByPodcastTags(List<Tag> tags, Pageable pageable) {
        return itemRepository.findAll(isInTags(tags), pageable);
    }

    @SuppressWarnings("unchecked")
    public Page<Item> findByTagsAndFullTextTerm(String term, List<Tag> tags, PageRequest page) {
        if (page.getSort().getOrderFor("pertinence") == null) {
            return itemRepository.findAll(getSearchSpecifications(term, tags), page);
        }

        return itemRepository.findAll(getSearchSpecifications(term, tags), new PageRequest(page.getPageNumber(), page.getPageSize()));
    }

    private Specifications<Item> getSearchSpecifications(String term, List<Tag> tags) {
        if (StringUtils.isEmpty(term)) {
            return where(isInTags(tags));
        }

        return where( hasId(itemRepository.fullTextSearch(term)) ).and( isInTags(tags) );
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

    @Transactional(readOnly = true)
    public List<Item> findByPodcast(Integer idPodcast) {
        return itemRepository.findByPodcast(podcastBusiness.findOne(idPodcast));
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
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return "";
    }


    public void reindex() throws InterruptedException {
        itemRepository.reindex();
    }

    public Item reset(Integer id) {
        Item itemToReset = findOne(id);

        if (itemDownloadManager.isInDownloadingQueue(itemToReset))
            return null;

        return save(itemToReset.reset());
    }
}
