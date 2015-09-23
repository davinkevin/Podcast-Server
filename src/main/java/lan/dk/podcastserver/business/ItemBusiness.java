package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.of;
import static lan.dk.podcastserver.repository.dsl.ItemDSL.getSearchSpecifications;

@Component
@Transactional
public class ItemBusiness {
    private static final String UPLOAD_PATTERN = "yyyy-MM-dd";

    final ItemDownloadManager itemDownloadManager;
    final PodcastServerParameters podcastServerParameters;
    final ItemRepository itemRepository;
    final PodcastBusiness podcastBusiness;
    final MimeTypeService mimeTypeService;

    @Autowired
    public ItemBusiness(ItemDownloadManager itemDownloadManager, PodcastServerParameters podcastServerParameters, ItemRepository itemRepository, PodcastBusiness podcastBusiness, MimeTypeService mimeTypeService) {
        this.itemDownloadManager = itemDownloadManager;
        this.podcastServerParameters = podcastServerParameters;
        this.itemRepository = itemRepository;
        this.podcastBusiness = podcastBusiness;
        this.mimeTypeService = mimeTypeService;
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Page<Item> findAll(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Page<Item> findByTagsAndFullTextTerm(String term, List<Tag> tags, Boolean downloaded, PageRequest page) {
        return page.getSort().getOrderFor("pertinence") == null
                ? itemRepository.findAll(getSearchSpecifications((StringUtils.isEmpty(term)) ? null : itemRepository.fullTextSearch(term), tags, downloaded), page)
                : findByTagsAndFullTextTermOrderByPertinence(term, tags, downloaded, page);
    }

    private Page<Item> findByTagsAndFullTextTermOrderByPertinence(String term, List<Tag> tags, Boolean downloaded, PageRequest page) {
        // List with the order of pertinence of search result :
        List<Integer> fullTextIdsWithOrder = itemRepository.fullTextSearch(term);

        // Reverse if order is ASC
        if ("ASC".equals(page.getSort().getOrderFor("pertinence").getDirection().toString())) {
            Collections.reverse(fullTextIdsWithOrder);
        }
        
        // List of all the item matching the search result :
        List<Item> allResult = (List<Item>) itemRepository.findAll(getSearchSpecifications(fullTextIdsWithOrder, tags, downloaded));

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

        return new PageImpl<>(orderedList, page, orderedList.size());
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

    @Transactional(readOnly = true)
    public Page<Item> findByPodcast(Integer idPodcast, PageRequest pageRequest) {
        return itemRepository.findByPodcast(idPodcast, pageRequest);
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

    public Item addItemByUpload(Integer podcastId, MultipartFile uploadedFile) throws IOException, URISyntaxException {
        Podcast podcast = podcastBusiness.findOne(podcastId);
        if (podcast == null) {
            throw new PodcastNotFoundException();
        }

        //TODO utiliser BEAN_UTIL pour faire du dynamique :
        // 1er temps : Template en dure : {title} - {date} - {title}.mp3

        Item item = new Item();
        String originalFilename = uploadedFile.getOriginalFilename();

        Path fileToSave = podcastServerParameters.rootFolder().resolve(podcast.getTitle()).resolve(originalFilename);
        Files.deleteIfExists(fileToSave);
        Files.createDirectories(fileToSave.getParent());

        uploadedFile.transferTo(fileToSave.toFile());

        item.setTitle(FilenameUtils.removeExtension(originalFilename.split(" - ")[2]))
                .setPubdate(fromFileName(originalFilename.split(" - ")[1]))
                .setUrl(UriComponentsBuilder.fromUri(podcastServerParameters.fileContainer()).pathSegment(podcast.getTitle()).pathSegment(originalFilename).build().toUriString())
                .setLength(uploadedFile.getSize())
                .setMimeType(mimeTypeService.getMimeType(FilenameUtils.getExtension(originalFilename)))
                .setDescription(podcast.getDescription())
                .setFileName(originalFilename)
                .setDownloadDate(of(LocalDateTime.now(), ZoneId.systemDefault()))
                .setPodcast(podcast)
                .setStatus(Status.FINISH);

        podcast.getItems().add(item);
        podcast.setLastUpdate(now());

        item = save(item);
        podcastBusiness.save(podcast);

        return item;
    }

    private ZonedDateTime fromFileName(String pubDate) {
        return of(LocalDateTime.of(LocalDate.parse(pubDate, DateTimeFormatter.ofPattern(UPLOAD_PATTERN)), LocalTime.of(0, 0)), ZoneId.systemDefault());
    }

}
