package lan.dk.podcastserver.business;

import javaslang.collection.List;
import javaslang.collection.Set;
import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.of;
import static lan.dk.podcastserver.repository.dsl.ItemDSL.getSearchSpecifications;

@Component
@Transactional
@RequiredArgsConstructor
public class ItemBusiness {

    private static final String UPLOAD_PATTERN = "yyyy-MM-dd";

    private final ItemDownloadManager itemDownloadManager;
    private final PodcastServerParameters podcastServerParameters;
    private final ItemRepository itemRepository;
    private final PodcastBusiness podcastBusiness;
    private final MimeTypeService mimeTypeService;

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Page<Item> findAll(Pageable pageable) {
        return itemRepository.findAll(pageable);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Page<Item> findByTagsAndFullTextTerm(String term, Set<Tag> tags, Boolean downloaded, Pageable page) {
        return page.getSort().getOrderFor("pertinence") == null
                ? itemRepository.findAll(getSearchSpecifications((StringUtils.isEmpty(term)) ? null : itemRepository.fullTextSearch(term), tags, downloaded), page)
                : findByTagsAndFullTextTermOrderByPertinence(term, tags, downloaded, page);
    }

    private Page<Item> findByTagsAndFullTextTermOrderByPertinence(String term, Set<Tag> tags, Boolean downloaded, Pageable page) {
        // List with the order of pertinence of search result :
        List<UUID> fullTextIdsWithOrder = itemRepository.fullTextSearch(term);

        // Reverse if order is ASC
        if ("ASC".equals(page.getSort().getOrderFor("pertinence").getDirection().toString())) {
            fullTextIdsWithOrder = fullTextIdsWithOrder.reverse();
        }
        
        // List of all the item matching the search result :
        List<Item> allResult = List.ofAll(itemRepository.findAll(getSearchSpecifications(fullTextIdsWithOrder, tags, downloaded)));

        //Re-order the result list : 
        List<Item> result = fullTextIdsWithOrder
                .map(id -> allResult.find(item -> id.equals(item.getId())))
                .filter(Option::isDefined)
                .map(Option::get)
                // Keep only the needed element for the page
                .drop(page.getOffset())
                .take(page.getPageSize());

        return new PageImpl<>(result.toJavaList(), page, result.length());
    }

    public Item save(Item entity) {
        return itemRepository.save(entity);
    }

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public Item findOne(UUID id) {
        return itemRepository.findOne(id);
    }

    public void delete(UUID id) {
        Item itemToDelete = findOne(id);

        //* Si le téléchargement est en cours ou en attente : *//
        itemDownloadManager.removeItemFromQueueAndDownload(itemToDelete);
        itemToDelete.getPodcast().getItems().remove(itemToDelete);
        itemRepository.delete(itemToDelete);
    }

    //****************************//

    @Transactional(readOnly = true)
    public Page<Item> findByPodcast(UUID idPodcast, Pageable pageable) {
        return itemRepository.findByPodcast(idPodcast, pageable);
    }

    public void reindex() throws InterruptedException {
        itemRepository.reindex();
    }

    public Item reset(UUID id) {
        Item itemToReset = findOne(id);

        if (itemDownloadManager.isInDownloadingQueue(itemToReset))
            return null;

        return save(itemToReset.reset());
    }

    public Item addItemByUpload(UUID podcastId, MultipartFile uploadedFile) throws IOException, URISyntaxException {
        Podcast podcast = podcastBusiness.findOne(podcastId);

        //TODO utiliser BEAN_UTIL pour faire du dynamique :
        // 1er temps : Template en dure : {title} - {date} - {title}.mp3

        Item item = new Item();
        String originalFilename = uploadedFile.getOriginalFilename();

        Path fileToSave = podcastServerParameters.getRootfolder().resolve(podcast.getTitle()).resolve(originalFilename);
        Files.deleteIfExists(fileToSave);
        Files.createDirectories(fileToSave.getParent());

        uploadedFile.transferTo(fileToSave.toFile());

        item.setTitle(FilenameUtils.removeExtension(originalFilename.split(" - ")[2]))
                .setPubDate(fromFileName(originalFilename.split(" - ")[1]))
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
