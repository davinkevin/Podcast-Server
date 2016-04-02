package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.*;
import lan.dk.podcastserver.service.factory.WGetFactory;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 19/03/2016 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class ParleysDownloaderTest {

    private static final JSONParser PARSER = new JSONParser();
    private static final String ROOT_FOLDER = "/tmp";
    private static final String ASSETS_URL = "https://cdn.parleys.com/p/5534a6b4e4b056a82338229d/9Wwo6oOVmk3_177812_60938.mp4?Signature=Rc-FUFrD81ypdP4RKtoAMr2E3RRf-dFYAbrW9jnAstX-R1S9-lgOrZBvpLRaVdTeOKMB-Td4tkag3doUIAjEcwFXzW3EWp-Zq0htHC0RhZWCb~LxVcsyzBHo6nYpyy0V4V--44CaumrgBV-~utssWmisLU5bzmhelySHyHTCrhtVo3CAZpSXsDBIWL7gK8jZ5pB61zJtHQiRMhSFC6bjcHlEJByftM83sr9R-U8GgdtTa6t1FRjCeVXYmKKG~1snsUz9mtSLYVrEGzxz3NojtJlVuAWtD7FQPEISHI1EMxDb9cWGoL8RijXVEjyfzUNmDJTwBNMKECwWM732DhUuBg__&Policy=eyJTdGF0ZW1lbnQiOlt7IlJlc291cmNlIjoiaHR0cHM6Ly9jZG4ucGFybGV5cy5jb20vcC81NTM0YTZiNGU0YjA1NmE4MjMzODIyOWQvOVd3bzZvT1ZtazNfMTc3ODEyXzYwOTM4Lm1wND9yZXNwb25zZS1jb250ZW50LWRpc3Bvc2l0aW9uPWF0dGFjaG1lbnQlM0JmaWxlbmFtZSUzRCUyNzlXd282b09WbWszXzE3NzgxMl82MDkzOC5tcDQlMjciLCJDb25kaXRpb24iOnsiRGF0ZUxlc3NUaGFuIjp7IkFXUzpFcG9jaFRpbWUiOjE0NDk0MDg1MzR9fX1dfQ__&Key-Pair-Id=APKAIUQAZRCNZDWJ4JJQ&response-content-disposition=attachment%3Bfilename%3D%279Wwo6oOVmk3_177812_60938.mp4%27";

    @Mock PodcastRepository podcastRepository;
    @Mock ItemRepository itemRepository;
    @Mock ItemDownloadManager itemDownloadManager;
    @Mock PodcastServerParameters podcastServerParameters;
    @Mock SimpMessagingTemplate template;
    @Mock MimeTypeService mimeTypeService;
    @Mock FfmpegService ffmpegService;
    @Mock UrlService urlService;
    @Mock WGetFactory wGetFactory;
    @Mock JsonService jsonService;
    @InjectMocks ParleysDownloader parleysDownloader;

    @Captor ArgumentCaptor<File> toConcatFiles;

    Podcast podcast;
    Item item;

    @Before
    public void beforeEach() {
        podcast = Podcast.builder()
                .id(UUID.randomUUID())
                .title("ParleysPodcast")
                .build();
        item = Item.builder()
                    .id(UUID.randomUUID())
                    .podcast(podcast)
                    .url("http://www.parleys.com/play/5534a6b4e4b056a82338229d")
                .build();

        when(podcastRepository.findOne(eq(podcast.getId()))).thenReturn(podcast);
        when(itemDownloadManager.getRootfolder()).thenReturn(ROOT_FOLDER);
        when(podcastServerParameters.getDownloadExtension()).thenReturn(".psdownload");
        when(urlService.newURL(anyString())).then(i -> Optional.of(new URL((String) i.getArguments()[0])));
        doAnswer(i -> {
            Files.createFile(File.class.cast(i.getArguments()[0]).toPath());
            return null;
        }).when(ffmpegService).concatDemux(any(File.class), anyVararg());

        parleysDownloader.postConstruct();
        parleysDownloader.setItem(item);

        FileSystemUtils.deleteRecursively(Paths.get(ROOT_FOLDER, podcast.getTitle()).toFile());
    }

    @Test
    public void should_download() throws MalformedURLException {
        /* Given */
        DownloadInfo mock = mock(DownloadInfo.class);
        WGet wget = mock(WGet.class);

        when(jsonService.from(eq(new URL("http://api.parleys.com/api/presentation.json/5534a6b4e4b056a82338229d?view=true")))).then(readerFrom("/remote/podcast/parleys/5534a6b4e4b056a82338229d.json"));
        when(wGetFactory.newDownloadInfo(anyString())).thenReturn(mock);
        when(wGetFactory.newWGet(any(DownloadInfo.class), any(File.class))).thenReturn(wget);

        /* When */
        parleysDownloader.download();

        /* Then */
        assertThat(parleysDownloader.target.toPath())
                .exists()
                .hasFileName("5534a6b4e4b056a82338229d.mp4")
                .hasParent(Paths.get(ROOT_FOLDER, podcast.getTitle()));
        assertThat(item.getStatus()).isSameAs(Status.FINISH);
        verify(ffmpegService, times(1)).concatDemux(eq(parleysDownloader.target), toConcatFiles.capture());
        assertThat(toConcatFiles.getAllValues()).hasSize(3)
                .containsExactly(
                        new File("/tmp/ParleysPodcast/9Wwo6oOVmk3_177812_60938.mp4"),
                        new File("/tmp/ParleysPodcast/X3yV1bXkPep_238750_2679062.mp4"),
                        new File("/tmp/ParleysPodcast/9Wwo6oOVmk3_2917812_8125.mp4")
                );
    }

    @Test
    public void should_not_found_any_item() {
        /* Given */
        item.setUrl("http://a.wrong.url/");
        /* When */
        parleysDownloader.download();

        /* Then */
        assertThat(item.getStatus()).isSameAs(Status.STOPPED);
    }

    @Test
    public void should_handle_assets_with_wrong_url() throws MalformedURLException {
         /* Given */
        DownloadInfo mock = mock(DownloadInfo.class);
        WGet wget = mock(WGet.class);

        when(jsonService.from(eq(new URL("http://api.parleys.com/api/presentation.json/5534a6b4e4b056a82338229d?view=true")))).then(readerFrom("/remote/podcast/parleys/5534a6b4e4b056a82338229d.json"));
        doThrow(MalformedURLException.class).when(wGetFactory).newDownloadInfo(eq(ASSETS_URL));
        when(wGetFactory.newDownloadInfo(not(eq(ASSETS_URL)))).thenReturn(mock);
        when(wGetFactory.newWGet(any(DownloadInfo.class), any(File.class))).thenReturn(wget);

        /* When */
        parleysDownloader.download();

        /* Then */
        assertThat(parleysDownloader.target.toPath())
                .exists()
                .hasFileName("5534a6b4e4b056a82338229d.mp4")
                .hasParent(Paths.get(ROOT_FOLDER, podcast.getTitle()));
        assertThat(item.getStatus()).isSameAs(Status.FINISH);
        verify(ffmpegService, times(1)).concatDemux(eq(parleysDownloader.target), toConcatFiles.capture());
        assertThat(toConcatFiles.getAllValues()).hasSize(2)
                .containsExactly(
                        new File("/tmp/ParleysPodcast/X3yV1bXkPep_238750_2679062.mp4"),
                        new File("/tmp/ParleysPodcast/9Wwo6oOVmk3_2917812_8125.mp4")
                );
    }

    @Test
    public void should_be_compatible() {
        assertThat(parleysDownloader.compatibility(item.getUrl())).isEqualTo(1);
    }

    private Answer<Optional<Object>> readerFrom(String url) {
        return i -> Optional.of(PARSER.parse(Files.newBufferedReader(Paths.get(ParleysDownloaderTest.class.getResource(url).toURI()))));
    }

}