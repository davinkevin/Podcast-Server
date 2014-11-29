package lan.dk.podcastserver.business;

import com.mysema.query.types.Predicate;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.utils.DateUtils;
import lan.dk.podcastserver.utils.MimeTypeUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static lan.dk.podcastserver.repository.Specification.ItemSpecifications.*;

@Component
@Transactional
public class ItemBusiness {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource ItemDownloadManager itemDownloadManager;

    @Resource ItemRepository itemRepository;
    @Resource PodcastBusiness podcastBusiness;

    @Value("${numberofdaytodownload:30}") Long numberOfDayToDownload;

    //** Delegation Repository **//
    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Item> findAll(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    @SuppressWarnings("unchecked")
    public Page<Item> findByTagsAndFullTextTerm(String term, List<Tag> tags, PageRequest page) {
        if (page.getSort().getOrderFor("pertinence") == null) {
            return itemRepository.findAll(getSearchSpecifications(term, tags), page);
        }

        return itemRepository.findAll(getSearchSpecifications(term, tags), new PageRequest(page.getPageNumber(), page.getPageSize()));
    }

    private Predicate getSearchSpecifications(String term, List<Tag> tags) {
        if (StringUtils.isEmpty(term)) {
            return isInTags(tags);
        }

        return isInId(itemRepository.fullTextSearch(term)).and( isInTags(tags) );
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

        delete(itemToDelete);
    }

    public void delete(Item entity) {
        itemRepository.delete(entity);
    }

    //****************************//

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Iterable<Item> findByStatus(String... status) {
        return itemRepository.findAll(hasStatus(status));
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Iterable<Item> findAllToDownload() {
        return itemRepository.findAll(isDownloaded(Boolean.FALSE)
                .and(isNewerThan(ZonedDateTime.now().minusDays(numberOfDayToDownload))));
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public Iterable<Item> findAllToDelete() {
        return itemRepository.findAll(isDownloaded(Boolean.TRUE)
                .and(isOlderThan(ZonedDateTime.now().minusDays(numberOfDayToDownload)))
                .and(hasToBeDeleted(Boolean.TRUE)));
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Page<Item> findByPodcast(Integer idPodcast, PageRequest pageRequest) {
        return itemRepository.findAll(isInPodcast(idPodcast), pageRequest);
    }



    public String getEpisodeFile(int id) {
        Item item = findOne(id);
        try {
            if (item.getLocalUrl() != null) {
                logger.info("Interne - Item " + id + " : " + item.getLocalUrl());
                URL redirectionURL = new URL(item.getLocalUrl());
                return new URI(redirectionURL.getProtocol(), null, redirectionURL.getHost(), redirectionURL.getPort(), redirectionURL.getPath(), null, null).toASCIIString();
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

    public Item addItemByUpload(Integer podcastId, MultipartFile uploadedFile) throws PodcastNotFoundException, IOException {
        Podcast podcast = podcastBusiness.findOne(podcastId);
        if (podcast == null) {
            throw new PodcastNotFoundException();
        }

        //TODO utiliser BEAN_UTIL pour faire du dynamique :
        // 1er temps : Template en dure : {title} - {date} - {title}.mp3

        Item item = new Item();
        //String name = name;
        File fileToSave = new File(podcastBusiness.getRootfolder() + File.separator + podcast.getTitle() + File.separator + uploadedFile.getOriginalFilename());
        if (fileToSave.exists()) {
            fileToSave.delete();
        }
        //noinspection ResultOfMethodCallIgnored
        fileToSave.mkdirs();

        uploadedFile.transferTo(fileToSave);

        item.setTitle(FilenameUtils.removeExtension(uploadedFile.getOriginalFilename().split(" - ")[2]))
                .setPubdate(DateUtils.fromFolder(uploadedFile.getOriginalFilename().split(" - ")[1]))
                .setUrl(podcastBusiness.getFileContainer() + "/" + podcast.getTitle() + "/" + uploadedFile.getOriginalFilename())
                .setLength(uploadedFile.getSize())
                .setMimeType(MimeTypeUtils.getMimeType(FilenameUtils.getExtension(uploadedFile.getOriginalFilename())))
                .setDescription(podcast.getDescription())
                .setLocalUrl(podcastBusiness.getFileContainer() + "/" + podcast.getTitle() + "/" + uploadedFile.getOriginalFilename())
                .setLocalUri(fileToSave.getAbsolutePath())
                .setDownloaddate(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()))
                .setPodcast(podcast)
                .setStatus("Finish");

        podcast.getItems().add(item);
        podcast.setLastUpdate(ZonedDateTime.now());

        item = itemRepository.save(item);
        podcastBusiness.save(podcast);

        return item;
    }
}
