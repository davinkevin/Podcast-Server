package lan.dk.podcastserver.business;

import com.mysema.query.types.Predicate;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.ItemAssert;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.manager.worker.updater.AbstractUpdater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.PodcastServerParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static lan.dk.podcastserver.repository.predicate.ItemPredicate.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 02/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemBusinessTest {

    String ROOT_FOLDER = "/tmp/podcast";

    @Mock ItemDownloadManager itemDownloadManager;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock ItemRepository itemRepository;
    @Mock PodcastBusiness podcastBusiness;
    @Mock MimeTypeService mimeTypeService;
    @InjectMocks ItemBusiness itemBusiness;

    @Before
    public void beforeEach() {
        itemBusiness.setItemDownloadManager(itemDownloadManager);
        FileSystemUtils.deleteRecursively(Paths.get(ROOT_FOLDER).toFile());
    }

    @Test
    public void should_find_all_by_page() {
        PageRequest pageRequest = new PageRequest(1, 3);
        PageImpl<Item> page = new PageImpl<>(new ArrayList<>());
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);

        /* When */
        Page<Item> pageResponse = itemBusiness.findAll(pageRequest);

        /* Then */
        assertThat(pageResponse)
                .isSameAs(page);

        verify(itemRepository, times(1)).findAll(eq(pageRequest));
    }

    @Test
    public void should_find_all_by_predicate() {
        /* Given */
        List<Item> items = new ArrayList<>();
        Predicate predicate = mock(Predicate.class);
        when(itemRepository.findAll(any(Predicate.class))).thenReturn(items);

        /* When */
        Iterable<Item> itemsWithStatus = itemBusiness.findAll(predicate);

        /* Then */
        assertThat(itemsWithStatus).isSameAs(items);
        verify(itemRepository, times(1)).findAll(eq(predicate));
    }

    @Test
    public void should_save() {
        /* Given */
        Item item = new Item();
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        /* When */
        Item savedItem = itemBusiness.save(item);
        /* Then */
        ItemAssert
                .assertThat(savedItem)
                .isSameAs(item);
        verify(itemRepository, times(1)).save(eq(item));
    }

    @Test
    public void should_find_by_id() {
        /* Given */
        Integer idOfItem = 33;
        Item item = new Item();
        when(itemRepository.findOne(anyInt())).thenReturn(item);
        /* When */
        Item savedItem = itemBusiness.findOne(idOfItem);
        /* Then */
        ItemAssert
                .assertThat(savedItem)
                .isSameAs(item);
        verify(itemRepository, times(1)).findOne(idOfItem);
    }

    @Test
    public void should_delete() {
        /* Given */
        Integer idOfItem = 33;
        Podcast podcast = new Podcast();
        podcast.setItems(new HashSet<>());
        Item item = new Item();
        item.setPodcast(podcast);
        podcast.getItems().add(item);

        when(itemRepository.findOne(anyInt())).thenReturn(item);

        /* When */
        itemBusiness.delete(idOfItem);
        /* Then */

        verify(itemRepository, times(1)).findOne(idOfItem);
        verify(itemDownloadManager, times(1)).removeItemFromQueueAndDownload(eq(item));
        verify(itemRepository, times(1)).delete(eq(item));
        assertThat(podcast.getItems()).isEmpty();
    }

    @Test
    public void should_reindex() throws InterruptedException {
        /* Given */
        /* When */
        itemBusiness.reindex();
        /* Then */
        verify(itemRepository, times(1)).reindex();
    }

    @Test
    public void should_reset_item() {
        /* Given */
        Integer itemId = 33;
        Item item = mock(Item.class);
        when(item.reset()).thenReturn(item);
        when(itemRepository.findOne(anyInt())).thenReturn(item);
        when(itemDownloadManager.isInDownloadingQueue(any(Item.class))).thenReturn(false);
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        /* When */
        Item resetedItem = itemBusiness.reset(itemId);

        /* Then */
        assertThat(resetedItem)
                .isSameAs(item);
        verify(itemRepository, times(1)).findOne(eq(itemId));
        verify(itemDownloadManager, times(1)).isInDownloadingQueue(eq(item));
        verify(item, times(1)).reset();
        verify(itemRepository, times(1)).save(eq(item));
    }

    @Test
    public void should_reset_a_downloading_item() {
        /* Given */
        Integer itemId = 33;
        Item item = mock(Item.class);
        when(item.reset()).thenReturn(item);
        when(itemRepository.findOne(anyInt())).thenReturn(item);
        when(itemDownloadManager.isInDownloadingQueue(any(Item.class))).thenReturn(true);

        /* When */
        Item resetedItem = itemBusiness.reset(itemId);

        /* Then */
        assertThat(resetedItem).isNull();
        verify(itemRepository, times(1)).findOne(eq(itemId));
        verify(itemDownloadManager, times(1)).isInDownloadingQueue(eq(item));
    }

    @Test
    public void should_find_by_status() {
        /* Given */
        List<Item> items = new ArrayList<>();
        when(itemRepository.findAll(any(Predicate.class))).thenReturn(items);

        /* When */
        Iterable<Item> itemsWithStatus = itemBusiness.findByStatus(Status.NOT_DOWNLOADED, Status.FINISH);

        /* Then */
        assertThat(itemsWithStatus).isSameAs(items);
        verify(itemRepository, times(1)).findAll(eq(hasStatus(Status.NOT_DOWNLOADED, Status.FINISH)));
    }

    @Test
    public void should_find_all_by_type_and_downloaded_before() {
        /* Given */
        List<Item> items = new ArrayList<>();
        when(itemRepository.findAll(any(Predicate.class))).thenReturn(items);
        AbstractUpdater.Type type = new AbstractUpdater.Type("RSS", "RSS");
        ZonedDateTime dateInPast = ZonedDateTime.now().minusMonths(3);

        /* When */
        Iterable<Item> itemsOfTypeRssAndDownloadedAfterDate = itemBusiness.findByTypeAndDownloadDateAfter(type, dateInPast);

        /* Then */
        assertThat(itemsOfTypeRssAndDownloadedAfterDate).isSameAs(items);
        verify(itemRepository, times(1)).findAll(eq(isOfType(type.key()).and(hasBeendDownloadedAfter(dateInPast))));
    }

    @Test
    public void should_find_all_to_download() {
        /* Given */
        List<Item> items = new ArrayList<>();
        when(itemRepository.findAll(any(Predicate.class))).thenReturn(items);
        when(podcastServerParameters.numberOfDayToDownload()).thenReturn(10L);
        /* When */
        Iterable<Item> itemsWithStatus = itemBusiness.findAllToDownload();

        /* Then */
        assertThat(itemsWithStatus).isSameAs(items);
        verify(itemRepository, times(1)).findAll(any(Predicate.class));
    }

    @Test
    public void should_find_all_to_delete() {
        /* Given */
        List<Item> items = new ArrayList<>();
        when(itemRepository.findAll(any(Predicate.class))).thenReturn(items);
        when(podcastServerParameters.numberOfDayToDownload()).thenReturn(10L);

        /* When */
        Iterable<Item> itemsWithStatus = itemBusiness.findAllToDelete();

        /* Then */
        assertThat(itemsWithStatus).isSameAs(items);
        verify(itemRepository, times(1)).findAll(any(Predicate.class));
    }

    @Test
    public void should_find_page_in_podcast() {
        /* Given */
        Integer idPodcast = 25;
        PageRequest pageRequest = new PageRequest(0, 20);
        PageImpl<Item> pageOfItem = new PageImpl<>(new ArrayList<>());
        when(itemRepository.findAll(any(Predicate.class), any(PageRequest.class))).thenReturn(pageOfItem);

        /* When */
        Page<Item> pageOfPodcast = itemBusiness.findByPodcast(idPodcast, pageRequest);

        /* Then */
        assertThat(pageOfPodcast.getContent())
                .isEqualTo(new ArrayList<>());
        verify(itemRepository, times(1)).findAll(eq(isInPodcast(25)), eq(pageRequest));
    }

    @Test(expected = PodcastNotFoundException.class)
    public void should_reject_because_no_podcast_found() throws IOException, URISyntaxException {
        itemBusiness.addItemByUpload(25, mock(MultipartFile.class));
    }

    @Test
    public void should_add_item_by_upload() throws IOException, URISyntaxException {
        /* Given */
        Integer idPodcast = 25;
        MultipartFile uploadedFile = mock(MultipartFile.class);
        Podcast podcast = new Podcast()
                .setDescription("aDescription");
        Long length = 123456789L;
        ROOT_FOLDER = "/tmp/podcast";
        podcast.setTitle("aPodcast");
        String aMimeType = "audio/type";
        String title = "aPodcast - 2015-09-10 - aTitle.mp3";
        Path ITEM_FILE_PATH = Paths.get(ROOT_FOLDER, podcast.getTitle(), title);
        Files.createDirectories(ITEM_FILE_PATH.getParent());
        Files.createFile(ITEM_FILE_PATH);

        when(podcastBusiness.findOne(eq(idPodcast))).thenReturn(podcast);
        when(uploadedFile.getOriginalFilename()).thenReturn(title);
        when(podcastServerParameters.rootFolder()).thenReturn(Paths.get(ROOT_FOLDER));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(podcastBusiness.save(any(Podcast.class))).then(i -> i.getArguments()[0]);
        when(podcastServerParameters.fileContainer()).thenReturn(new URI("http://localhost:8080/podcast"));
        when(uploadedFile.getSize()).thenReturn(length);
        when(mimeTypeService.getMimeType(anyString())).thenReturn(aMimeType);
        /* When */
        Item item = itemBusiness.addItemByUpload(idPodcast, uploadedFile);

        /* Then */
        ItemAssert
                .assertThat(item)
                .hasTitle("aTitle")
                .hasPubdate(ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(title.split(" - ")[1], DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.of(0, 0)), ZoneId.systemDefault()))
                .hasUrl("http://localhost:8080/podcast/aPodcast/" + title)
                .hasLength(length)
                .hasMimeType(aMimeType)
                .hasDescription("aDescription")
                .hasFileName(title)
                .hasPodcast(podcast)
                .hasStatus(Status.FINISH.value());

        assertThat(podcast.getItems()).contains(item);
        verify(podcastServerParameters, times(1)).rootFolder();
        verify(podcastServerParameters, times(1)).fileContainer();
        verify(mimeTypeService, times(1)).getMimeType(eq("mp3"));
        verify(podcastBusiness, times(1)).findOne(eq(idPodcast));
        verify(podcastBusiness, times(1)).save(eq(podcast));
        verify(itemRepository, times(1)).save(eq(item));
    }
}