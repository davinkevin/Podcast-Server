package lan.dk.podcastserver.business;

import com.mysema.query.types.Predicate;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.predicate.ItemPredicate;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static lan.dk.podcastserver.repository.predicate.ItemPredicate.*;

@Component
@Transactional
public class ItemBusiness {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String UPLOAD_PATTERN = "yyyy-MM-dd";

    ItemDownloadManager itemDownloadManager;
    final PodcastServerParameters podcastServerParameters;
    final ItemRepository itemRepository;
    final PodcastBusiness podcastBusiness;
    final MimeTypeService mimeTypeService;

    @Autowired
    public ItemBusiness(PodcastServerParameters podcastServerParameters, ItemRepository itemRepository, PodcastBusiness podcastBusiness, MimeTypeService mimeTypeService) {
        this.podcastServerParameters = podcastServerParameters;
        this.itemRepository = itemRepository;
        this.podcastBusiness = podcastBusiness;
        this.mimeTypeService = mimeTypeService;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Page<Item> findAll(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    public Iterable findAll(Predicate predicate) {
        return itemRepository.findAll(predicate);
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Page<Item> findByTagsAndFullTextTerm(String term, List<Tag> tags, PageRequest page) {
        return page.getSort().getOrderFor("pertinence") == null 
                ? itemRepository.findAll(getSearchSpecifications(term, tags), page) 
                : findByTagsAndFullTextTermOrderByPertinence(term, tags, page);
    }

    @SuppressWarnings("unchecked")
    private Page<Item> findByTagsAndFullTextTermOrderByPertinence(String term, List<Tag> tags, PageRequest page) {
        // List with the order of pertinence of search result :
        List<Integer> fullTextIdsWithOrder = itemRepository.fullTextSearch(term);

        // Reverse if order is ASC
        if ("ASC".equals(page.getSort().getOrderFor("pertinence").getDirection().toString())) {
            Collections.reverse(fullTextIdsWithOrder);
        }
        
        // List of all the item matching the search result :
        List<Item> allResult = (List<Item>) itemRepository.findAll(ItemPredicate.getSearchSpecifications(fullTextIdsWithOrder, tags));

        //Number of result
        Long numberOfResult = (long) allResult.size();
        
        //Re-order the result list : 
        List<Item> orderedList = fullTextIdsWithOrder
                .stream()
                .map(id -> allResult.stream().filter(item -> id.equals(item.getId())).findFirst())
                .filter(Optional::isPresent)
                .map(Optional::get)
                // Keep only the needed element for the page
                .skip(page.getOffset())
                .limit(page.getPageSize())
                // Collect them all !
                .collect(Collectors.toList());

        return new PageImpl<>(orderedList, page, numberOfResult);
    }

    public Item save(Item entity) {
        return itemRepository.save(entity);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Item findOne(Integer integer) {
        return itemRepository.findOne(integer);
    }

    public void delete(Integer id) {
        Item itemToDelete = findOne(id);

        //* Si le téléchargement est en cours ou en attente : *//
        itemDownloadManager.removeItemFromQueueAndDownload(itemToDelete);
        itemToDelete.getPodcast().getItems().remove(itemToDelete);
        itemRepository.delete(itemToDelete);
    }

    //****************************//

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Iterable<Item> findByStatus(Status... status) {
        return itemRepository.findAll(hasStatus(status));
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Iterable<Item> findAllToDownload() {
        return itemRepository.findAll(isDownloaded(Boolean.FALSE)
                .and(isNewerThan(ZonedDateTime.now().minusDays(podcastServerParameters.numberOfDayToDownload()))));
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Iterable<Item> findAllToDelete() {
        return itemRepository.findAll(isDownloaded(Boolean.TRUE)
                .and(hasBeenDownloadedBefore(ZonedDateTime.now().minusDays(podcastServerParameters.numberOfDayToDownload())))
                .and(hasToBeDeleted(Boolean.TRUE)));
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    public Page<Item> findByPodcast(Integer idPodcast, PageRequest pageRequest) {
        return itemRepository.findAll(isInPodcast(idPodcast), pageRequest);
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

    public Item addItemByUpload(Integer podcastId, MultipartFile uploadedFile) throws PodcastNotFoundException, IOException, URISyntaxException {
        Podcast podcast = podcastBusiness.findOne(podcastId);
        if (podcast == null) {
            throw new PodcastNotFoundException();
        }

        //TODO utiliser BEAN_UTIL pour faire du dynamique :
        // 1er temps : Template en dure : {title} - {date} - {title}.mp3

        Item item = new Item();
        //String name = name;
        String originalFilename = uploadedFile.getOriginalFilename();
        File fileToSave = new File(podcastServerParameters.getRootfolder() + File.separator + podcast.getTitle() + File.separator + originalFilename);
        if (fileToSave.exists()) {
            fileToSave.delete();
        }
        //noinspection ResultOfMethodCallIgnored
        fileToSave.getParentFile().mkdirs();

        uploadedFile.transferTo(fileToSave);



        item.setTitle(FilenameUtils.removeExtension(originalFilename.split(" - ")[2]))
                .setPubdate(fromFileName(originalFilename.split(" - ")[1]))
                .setUrl(UriComponentsBuilder.fromUri(podcastServerParameters.fileContainer()).pathSegment(podcast.getTitle()).pathSegment(originalFilename).build().toUriString())
                .setLength(uploadedFile.getSize())
                .setMimeType(mimeTypeService.getMimeType(FilenameUtils.getExtension(originalFilename)))
                .setDescription(podcast.getDescription())
                .setFileName(originalFilename)
                .setDownloadDate(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()))
                .setPodcast(podcast)
                .setStatus(Status.FINISH);

        podcast.getItems().add(item);
        podcast.setLastUpdate(ZonedDateTime.now());

        item = itemRepository.save(item);
        podcastBusiness.save(podcast);

        return item;
    }

    private ZonedDateTime fromFileName(String pubDate) {
        return ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(pubDate, DateTimeFormatter.ofPattern(UPLOAD_PATTERN)), LocalTime.of(0, 0)), ZoneId.systemDefault());
    }

    private Predicate getSearchSpecifications(String term, List<Tag> tags) {
        return (StringUtils.isEmpty(term))
                ? isInTags(tags)
                : ItemPredicate.getSearchSpecifications(itemRepository.fullTextSearch(term), tags);
    }

    @Autowired
    public void setItemDownloadManager(ItemDownloadManager itemDownloadManager) {
        this.itemDownloadManager = itemDownloadManager;
    }
}
