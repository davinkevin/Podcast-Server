package lan.dk.podcastserver.business;

import com.github.davinkevin.podcastserver.service.MimeTypeService;
import com.querydsl.core.types.Predicate;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.entity.Tag;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.*;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static io.vavr.API.Set;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 02/08/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemBusinessTest {

    private String ROOT_FOLDER = "/tmp/podcast";

    private @Mock ItemDownloadManager itemDownloadManager;
    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock ItemRepository itemRepository;
    private @Mock PodcastBusiness podcastBusiness;
    private @Mock    MimeTypeService mimeTypeService;
    private @InjectMocks ItemBusiness itemBusiness;

    @Before
    public void beforeEach() {
        FileSystemUtils.deleteRecursively(Paths.get(ROOT_FOLDER).toFile());
    }

    @Test
    public void should_find_all_by_page() {
        /* Given */
        PageRequest pageRequest = PageRequest.of(1, 3);
        PageImpl<Item> page = new PageImpl<>(new ArrayList<>());
        when(itemRepository.findAll(any(Pageable.class))).thenReturn(page);

        /* When */
        Page<Item> pageResponse = itemBusiness.findAll(pageRequest);

        /* Then */
        PageAssert.assertThat(pageResponse).isSameAs(page);
        verify(itemRepository, times(1)).findAll(eq(pageRequest));
    }

    @Test
    public void should_save() {
        /* Given */
        Item item = new Item();
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        /* When */
        Item savedItem = itemBusiness.save(item);
        /* Then */
        assertThat(savedItem).isSameAs(item);
        verify(itemRepository, times(1)).save(eq(item));
    }

    @Test
    public void should_find_by_id() {
        /* Given */
        UUID idOfItem = UUID.randomUUID();
        Item item = new Item();
        when(itemRepository.findById(any(UUID.class))).thenReturn(Optional.of(item));
        /* When */
        Item savedItem = itemBusiness.findOne(idOfItem);
        /* Then */
        assertThat(savedItem).isSameAs(item);
        verify(itemRepository, times(1)).findById(idOfItem);
    }

    @Test
    public void should_delete() {
        /* Given */
        UUID idOfItem = UUID.randomUUID();
        Podcast podcast = new Podcast();
        podcast.setItems(new HashSet<>());
        Item item = new Item();
        item.setPodcast(podcast);
        podcast.getItems().add(item);

        when(itemRepository.findById(any(UUID.class))).thenReturn(Optional.of(item));

        /* When */
        itemBusiness.delete(idOfItem);
        /* Then */

        verify(itemRepository, times(1)).findById(idOfItem);
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
        UUID itemId = UUID.randomUUID();
        Item item = mock(Item.class);
        when(item.reset()).thenReturn(item);
        when(itemRepository.findById(any(UUID.class))).thenReturn(Optional.of(item));
        when(itemDownloadManager.isInDownloadingQueue(any(Item.class))).thenReturn(false);
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        /* When */
        Item resetedItem = itemBusiness.reset(itemId);

        /* Then */
        assertThat(resetedItem).isSameAs(item);
        verify(itemRepository, times(1)).findById(eq(itemId));
        verify(itemDownloadManager, times(1)).isInDownloadingQueue(eq(item));
        verify(item, times(1)).reset();
        verify(itemRepository, times(1)).save(eq(item));
    }

    @Test
    public void should_reset_a_downloading_item() {
        /* Given */
        UUID itemId = UUID.randomUUID();
        Item item = mock(Item.class);
        when(itemRepository.findById(any(UUID.class))).thenReturn(Optional.of(item));
        when(itemDownloadManager.isInDownloadingQueue(any(Item.class))).thenReturn(true);

        /* When */
        Item resetedItem = itemBusiness.reset(itemId);

        /* Then */
        assertThat(resetedItem).isNull();
        verify(itemRepository, times(1)).findById(eq(itemId));
        verify(itemDownloadManager, times(1)).isInDownloadingQueue(eq(item));
    }

    @Test
    public void should_find_page_in_podcast() {
        /* Given */
        UUID idPodcast = UUID.randomUUID();
        PageRequest pageRequest = PageRequest.of(0, 20);
        PageImpl<Item> pageOfItem = new PageImpl<>(new ArrayList<>());
        when(itemRepository.findByPodcast(any(UUID.class), any(PageRequest.class))).thenReturn(pageOfItem);

        /* When */
        Page<Item> pageOfPodcast = itemBusiness.findByPodcast(idPodcast, pageRequest);

        /* Then */
        assertThat(pageOfPodcast.getContent()).isEqualTo(new ArrayList<>());
        verify(itemRepository, times(1)).findByPodcast(eq(idPodcast), eq(pageRequest));
    }

    @Test
    public void should_add_item_by_upload() throws IOException, URISyntaxException {
        /* Given */
        UUID idPodcast = UUID.randomUUID();
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
        when(podcastServerParameters.getRootfolder()).thenReturn(Paths.get(ROOT_FOLDER));
        when(itemRepository.save(any(Item.class))).then(i -> i.getArguments()[0]);
        when(podcastBusiness.save(any(Podcast.class))).then(i -> i.getArguments()[0]);
        when(uploadedFile.getSize()).thenReturn(length);
        when(mimeTypeService.getMimeType(anyString())).thenReturn(aMimeType);
        /* When */
        Item item = itemBusiness.addItemByUpload(idPodcast, uploadedFile);

        /* Then */
        assertThat(item)
                .hasTitle("aTitle")
                .hasPubDate(ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(title.split(" - ")[1], DateTimeFormatter.ofPattern("yyyy-MM-dd")), LocalTime.of(0, 0)), ZoneId.systemDefault()))
                .hasUrl(null)
                .hasLength(length)
                .hasMimeType(aMimeType)
                .hasDescription("aDescription")
                .hasFileName(title)
                .hasPodcast(podcast)
                .hasStatus(Status.FINISH);

        assertThat(podcast.getItems()).contains(item);
        verify(podcastServerParameters, times(1)).getRootfolder();
        verify(mimeTypeService, times(1)).getMimeType(eq("mp3"));
        verify(podcastBusiness, times(1)).findOne(eq(idPodcast));
        verify(podcastBusiness, times(1)).save(eq(podcast));
        verify(itemRepository, times(1)).save(eq(item));
    }

    @Test
    public void should_find_by_tags_and_full_text_without_specific_order() {
        /* Given */
        String term = "Foo";
        Set<Tag> tags = io.vavr.collection.HashSet.of(new Tag().setName("Discovery"), new Tag().setName("Fun"));
        PageRequest pageRequest = PageRequest.of(1, 3, Sort.Direction.fromString("DESC"), "title");
        PageImpl<Item> pageResponse = new PageImpl<>(new ArrayList<>());

        when(itemRepository.fullTextSearch(eq(term))).thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        when(itemRepository.findAll(any(Predicate.class), any(PageRequest.class))).thenReturn(pageResponse);

        /* When */
        Page<Item> byTagsAndFullTextTerm = itemBusiness.findByTagsAndFullTextTerm(term, tags, Set(Status.FINISH), pageRequest);

        /* Then */
        PageAssert.assertThat(byTagsAndFullTextTerm).isSameAs(pageResponse);
        verify(itemRepository, times(1)).fullTextSearch(eq(term));
        verify(itemRepository, times(1)).findAll(any(Predicate.class), eq(pageRequest));
    }

    @Test
    public void should_find_by_tags() {
        /* Given */
        Set<Tag> tags = io.vavr.collection.HashSet.of(new Tag().setName("Discovery"), new Tag().setName("Fun"));
        PageRequest pageRequest = PageRequest.of(1, 3, Sort.Direction.fromString("DESC"), "title");
        PageImpl<Item> pageResponse = new PageImpl<>(new ArrayList<>());

        when(itemRepository.findAll(any(Predicate.class), any(PageRequest.class))).thenReturn(pageResponse);

        /* When */
        Page<Item> byTagsAndFullTextTerm = itemBusiness.findByTagsAndFullTextTerm("", tags, Set(Status.FINISH), pageRequest);

        /* Then */
        PageAssert.assertThat(byTagsAndFullTextTerm).isSameAs(pageResponse);
        verify(itemRepository, times(1)).findAll(any(Predicate.class), eq(pageRequest));
    }

    @Test
    public void should_find_by_tags_and_full_text_with_pertinence_order_asc() {
        /* Given */
        String term = "Foo";
        Set<Tag> tags = io.vavr.collection.HashSet.of(new Tag().setName("Discovery"), new Tag().setName("Fun"));
        PageRequest pageRequest = PageRequest.of(1, 3, Sort.Direction.fromString("ASC"), "pertinence");
        List<Item> itemsFrom1To20 = IntStream.range(1, 20)
                .mapToObj(id -> new Item().setId(UUID.randomUUID()))
                .collect(List.collector());

        when(itemRepository.fullTextSearch(eq(term))).thenReturn(itemsFrom1To20.map(Item::getId));
        when(itemRepository.findAll(any(Predicate.class))).thenReturn(itemsFrom1To20);

        /* When */
        Page<Item> pageOfItem = itemBusiness.findByTagsAndFullTextTerm(term, tags, Set(Status.FINISH), pageRequest);

        /* Then */
        assertThat(pageOfItem.getContent()).contains(itemsFrom1To20.get(15),itemsFrom1To20.get(14),itemsFrom1To20.get(13));
        verify(itemRepository, times(1)).fullTextSearch(eq(term));
        verify(itemRepository, times(1)).findAll(any(Predicate.class));
    }
}
