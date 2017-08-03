package lan.dk.podcastserver.manager.worker.downloader;

import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDownloader implements Runnable, Downloader {

    static final String WS_TOPIC_DOWNLOAD = "/topic/download";

    @Getter @Setter @Accessors(chain = true) protected Item item;
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
        Try.of(this::download)
            .onFailure(e -> log.error("Error during download", e))
            .onFailure(e -> this.stopDownload());
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

    @Override
    @Transactional
    public void finishDownload() {
        itemDownloadManager.removeACurrentDownload(item);

        if (isNull(target)) {
            resetDownload();
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

    public void resetDownload() {
        stopDownload();
    }

    @Transactional
    public Path getTargetFile(Item item) {

        if (nonNull(target)) return target;

        Path finalFile = getDestinationFile(item);
        log.debug("Creation of file : {}", finalFile.toFile().getAbsolutePath());

        try {
            if (Files.notExists(finalFile.getParent())) Files.createDirectory(finalFile.getParent());

            if (!(Files.exists(finalFile) || Files.exists(finalFile.resolveSibling(finalFile.getFileName() + temporaryExtension)))) {
                return finalFile.resolveSibling(finalFile.getFileName() + temporaryExtension);
            }

            log.info("Doublon sur le fichier en lien avec {} - {}, {}", item.getPodcast().getTitle(), item.getId(), item.getTitle() );
            String fileName = finalFile.getFileName().toString();
            return Files.createTempFile(finalFile.getParent(), FilenameUtils.getBaseName(fileName) + "-", "." + FilenameUtils.getExtension(fileName) + temporaryExtension);
        } catch (IOException e) {
            log.error("Error during creation of target file", e);
            stopDownload();
            return null;
        }
    }

    private Path getDestinationFile(Item item) {
        String fileName = FilenameUtils.getName(StringUtils.substringBefore(getItemUrl(item), "?"));
        return  podcastServerParameters.getRootfolder().resolve(item.getPodcast().getTitle()).resolve(fileName);
    }

    @Transactional
    protected void saveSyncWithPodcast() {
        Try.run(() -> {
            item.setPodcast(podcastRepository.findOne(item.getPodcast().getId()));
            itemRepository.save(item);
        })
            .onFailure(e -> log.error("Error during save and Sync of the item {}", item, e));
    }

    @Transactional
    void convertAndSaveBroadcast() {
        template.convertAndSend(WS_TOPIC_DOWNLOAD, item);
    }

    public String getItemUrl(Item item) {
        return item.getUrl();
    }

    @PostConstruct
    public void postConstruct() {
        temporaryExtension = podcastServerParameters.getDownloadExtension();
        hasTempExtensionMatcher = FileSystems.getDefault().getPathMatcher("glob:*" + temporaryExtension);
    }
}
