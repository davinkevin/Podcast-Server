package lan.dk.podcastserver.manager.downloader;


import com.github.davinkevin.podcastserver.service.MimeTypeService;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.vavr.API.Option;
import static io.vavr.API.Try;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDownloader implements Runnable, Downloader {

    static final String WS_TOPIC_DOWNLOAD = "/topic/download";

    @Getter protected Item item;
    @Getter @Accessors(chain = true) protected DownloadingItem downloadingItem;
    @Setter @Accessors(chain = true) protected ItemDownloadManager itemDownloadManager;

    protected final ItemRepository itemRepository;
    protected final PodcastRepository podcastRepository;
    protected final PodcastServerParameters podcastServerParameters;
    protected final SimpMessagingTemplate template;
    protected final MimeTypeService mimeTypeService;

    String temporaryExtension;
    protected Path target;
    private PathMatcher hasTempExtensionMatcher;
    AtomicBoolean stopDownloading = new AtomicBoolean(false);


    @Override
    public AbstractDownloader setDownloadingItem(DownloadingItem downloadingItem) {
        this.downloadingItem = downloadingItem;
        this.item = downloadingItem.getItem();
        return this;
    }

    @Override
    public void run() {
        log.debug("Run");
        startDownload();
    }

    @Override
    public void startDownload() {
        item.setStatus(Status.STARTED);
        stopDownloading.set(false);
        saveSyncWithPodcast();
        convertAndSaveBroadcast();
        Try(this::download)
            .onFailure(e -> log.error("Error during download", e))
            .onFailure(e -> this.failDownload());
    }

    @Override
    public void pauseDownload() {
        item.setStatus(Status.PAUSED);
        stopDownloading.set(true);
        saveSyncWithPodcast();
        convertAndSaveBroadcast();
    }

    @Override
    public void stopDownload() {
        item.setStatus(Status.STOPPED);
        stopDownloading.set(true);
        saveSyncWithPodcast();
        itemDownloadManager.removeACurrentDownload(item);
        if (nonNull(target)) Try.run(() -> Files.deleteIfExists(target));
        convertAndSaveBroadcast();
    }

    public void failDownload() {
        item.setStatus(Status.FAILED);
        stopDownloading.set(true);
        item.addATry();
        saveSyncWithPodcast();
        itemDownloadManager.removeACurrentDownload(item);
        if (nonNull(target)) Try.run(() -> Files.deleteIfExists(target));
        convertAndSaveBroadcast();
    }

    @Override
    @Transactional
    public void finishDownload() {
        itemDownloadManager.removeACurrentDownload(item);

        if (isNull(target)) {
            failDownload();
            return;
        }

        item.setStatus(Status.FINISH);

        Try.run(() -> {
            if (hasTempExtensionMatcher.matches(target.getFileName())) {
                Path targetWithoutExtension = target.resolveSibling(target.getFileName().toString().replace(temporaryExtension, ""));

                Files.deleteIfExists(targetWithoutExtension);
                Files.move(target, targetWithoutExtension);

                target = targetWithoutExtension;
            }
            item
                .setLength(Files.size(target))
                .setMimeType(mimeTypeService.probeContentType(target));
        });

        item.setFileName(FilenameUtils.getName(target.getFileName().toString()));
        item.setDownloadDate(ZonedDateTime.now());

        saveSyncWithPodcast();
        convertAndSaveBroadcast();
    }

    @Transactional
    public Path getTargetFile(Item item) {

        if (nonNull(target)) return target;

        Path finalFile = getDestinationFile(item);
        log.debug("Creation of file : {}", finalFile.toFile().getAbsolutePath());

        try {
            if (Files.notExists(finalFile.getParent())) Files.createDirectories(finalFile.getParent());

            if (!(Files.exists(finalFile) || Files.exists(finalFile.resolveSibling(finalFile.getFileName() + temporaryExtension)))) {
                return finalFile.resolveSibling(finalFile.getFileName() + temporaryExtension);
            }

            log.info("Doublon sur le fichier en lien avec {} - {}, {}", item.getPodcast().getTitle(), item.getId(), item.getTitle() );
            return generateTempFileNextTo(finalFile);
        } catch (UncheckedIOException | IOException e) {
            log.error("Error during creation of target file", e);
            stopDownload();
            return null;
        }
    }

    Path generateTempFileNextTo(Path finalFile) {
        String fileName = finalFile.getFileName().toString();
        return Try.of(() -> Files.createTempFile(finalFile.getParent(), FilenameUtils.getBaseName(fileName) + "-", "." + FilenameUtils.getExtension(fileName) + temporaryExtension))
                .getOrElseThrow(e -> new UncheckedIOException(IOException.class.cast(e)));
    }

    private Path getDestinationFile(Item item) {
        String fileName = Option(downloadingItem.getFilename()).getOrElse(() -> getFileName(item));
        return  podcastServerParameters.getRootfolder().resolve(item.getPodcast().getTitle()).resolve(fileName);
    }

    @Transactional
    protected void saveSyncWithPodcast() {
        Try.run(() -> {
            Podcast podcast = podcastRepository.findById(item.getPodcast().getId()).orElseThrow(() -> new Error("Item with ID "+ item.getPodcast().getId() +" not found"));
            item.setPodcast(podcast);
            itemRepository.save(item);
        })
            .onFailure(e -> log.error("Error during save and Sync of the item {}", item, e));
    }

    @Transactional
    void convertAndSaveBroadcast() {
        template.convertAndSend(WS_TOPIC_DOWNLOAD, item);
    }

    public String getItemUrl(Item item) {
        return downloadingItem.url().getOrElse(item::getUrl);
    }

    @PostConstruct
    public void postConstruct() {
        temporaryExtension = podcastServerParameters.getDownloadExtension();
        hasTempExtensionMatcher = FileSystems.getDefault().getPathMatcher("glob:*" + temporaryExtension);
    }
}
