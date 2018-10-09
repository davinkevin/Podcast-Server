package lan.dk.podcastserver.manager.downloader;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VGetParser;
import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.URLInfo;
import com.github.axet.wget.info.ex.DownloadError;
import com.github.axet.wget.info.ex.DownloadIOCodeError;
import com.github.axet.wget.info.ex.DownloadInterruptedError;
import com.github.axet.wget.info.ex.DownloadMultipartError;
import com.github.davinkevin.podcastserver.service.MimeTypeService;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import com.github.davinkevin.podcastserver.service.FfmpegService;
import com.github.davinkevin.podcastserver.service.factory.WGetFactory;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import static io.vavr.API.Option;
import static io.vavr.API.Try;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.time.ZonedDateTime.now;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Created by kevin on 14/12/2013 for Podcast Server
 */

@Slf4j
@Scope("prototype")
@Component("YoutubeDownloader")
public class YoutubeDownloader extends AbstractDownloader {

    private static final String DEFAULT_EXTENSION_MP4 = "mp4";
    private static final String ERROR_NO_CONTENT_TYPE = "Content Type %s not found for video %s at url %s";
    VGet v = null;

    final YoutubeWatcher watcher = new YoutubeWatcher(this);

    private final WGetFactory wGetFactory;
    private final FfmpegService ffmpegService;

    public YoutubeDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, WGetFactory wGetFactory, FfmpegService ffmpegService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService);
        this.wGetFactory = wGetFactory;
        this.ffmpegService = ffmpegService;
    }

    @Override
    public Item download() {
        try {
            VGetParser parser = wGetFactory.parser(item.getUrl());
            v = wGetFactory.newVGet(parser.info(new URL(item.getUrl())));

            v.extract(parser, stopDownloading, watcher);

            target = Option(getTargetFile(item)).getOrElseThrow(() -> new RuntimeException("target is undefined for download of " + item.getUrl()));
            v
                .getVideo()
                .getInfo()
                .forEach(vi -> vi.setTarget(generatePartFile(target, vi).toFile()));

            v.download(parser, stopDownloading, watcher);
        } catch (DownloadInterruptedError e) {
            log.debug("Stopped", e);
        } catch (DownloadMultipartError e) {
            e.getInfo()
                .getParts()
                .stream()
                .map(DownloadInfo.Part::getException)
                .filter(Objects::nonNull)
                .forEach(Throwable::printStackTrace);

            throw new RuntimeException(e);
        } catch (StringIndexOutOfBoundsException | MalformedURLException | NullPointerException | DownloadError e) {
            throw new RuntimeException(e);
        }

        log.debug("Download ended");
        return item;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private Path generatePartFile(Path targetFile, VideoFileInfo vi) {
        return targetFile.resolveSibling(targetFile.getFileName() + v.getContentExt(vi));
    }

    @Override
    public String getFileName(Item item) {
        return Option(v)
                .map(VGet::getVideo)
                .map(VideoInfo::getTitle)
                .map(s -> s.replaceAll("[^a-zA-Z0-9.-]", "_"))
                .getOrElseThrow(() -> new RuntimeException("Error during creation of filename of " + item.getUrl()));
    }

    @Override
    public void pauseDownload() {
        item.setStatus(Status.PAUSED);
        saveSyncWithPodcast();
        convertAndSaveBroadcast();
    }

    @Override
    public void restartDownload() {
        item.setStatus(Status.STARTED);
        saveSyncWithPodcast();
        convertAndSaveBroadcast();
        synchronized (watcher) { watcher.notifyAll(); }
    }

    @Override
    public void stopDownload() {
        if (item.getStatus() == Status.PAUSED) {
            synchronized (watcher) { watcher.notify(); }
        }
        super.stopDownload();

        if (nonNull(v) && nonNull(v.getVideo()) && nonNull(v.getVideo().getInfo()))
            v.getVideo()
                .getInfo()
                .stream()
                .filter(v -> nonNull(v.targetFile))
                .map(v -> v.targetFile.toPath())
                .forEach(p -> Try(() -> Files.deleteIfExists(p)));
    }

    @Override
    public void finishDownload() {

        try {
            Path fileWithExtension = target.resolveSibling(getDefinitiveFileName());
            Files.deleteIfExists(target);

            if (hasOnlyOneStream()) {
                target = Files.move(v.getVideo().getInfo().get(0).targetFile.toPath(), fileWithExtension, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Path audioFile = getStream("audio");
                Path video = getStream("video");

                target = ffmpegService.mergeAudioAndVideo(video, audioFile, fileWithExtension);

                Files.deleteIfExists(video);
                Files.deleteIfExists(audioFile);
            }
        } catch (Exception e) {
            log.error("Error during specific move", e);
            failDownload();
            throw new RuntimeException("Error during specific move", e);
        }

        super.finishDownload();
    }

    private boolean hasOnlyOneStream() {
        return v.getVideo().getInfo().size() == 1;
    }

    private Path getStream(String type) {
        return v.getVideo().getInfo().stream()
                .filter(v -> v.getContentType().contains(type))
                .map(v -> v.targetFile.toPath())
                .findFirst()
                .orElseThrow(() -> new RuntimeException(format(ERROR_NO_CONTENT_TYPE, type, item.getTitle(), item.getUrl())));
    }

    @Override
    public Integer compatibility(DownloadingItem ditem) {
        return ditem.getUrls().length() == 1 && ditem.getUrls().head().contains("www.youtube.com")
                ? 1
                : Integer.MAX_VALUE;
    }

    private String getDefinitiveFileName() {
        String videoExt = v.getVideo().getInfo().stream()
                .map(VideoFileInfo::getContentType)
                .filter(c -> c.contains("video"))
                .map(c -> StringUtils.substringAfter(c, "/"))
                .findFirst()
                .orElse(DEFAULT_EXTENSION_MP4);

        return target.getFileName().toString().replace(temporaryExtension, "." + videoExt);
    }

    @Slf4j
    static class YoutubeWatcher implements Runnable {

        private final YoutubeDownloader youtubeDownloader;
        private final ZonedDateTime launchDateDownload = now();
        private Long globalSize = null;
        Integer MAX_WAITING_MINUTE = 5;

        public YoutubeWatcher(YoutubeDownloader youtubeDownloader) {
            this.youtubeDownloader = youtubeDownloader;
        }

        @Override
        public void run() {
            VideoInfo info = youtubeDownloader.v.getVideo();
            List<VideoFileInfo> downloadInfo = info.getInfo();
            Item item = youtubeDownloader.item;

            if (item.getStatus() == Status.PAUSED) {
                synchronized (this) { Try.run(this::wait); }
            }

            switch (info.getState()) {
                case EXTRACTING_DONE:
                    log.debug(FilenameUtils.getName(valueOf(item.getUrl())) + " " + info.getState());
                    break;
                case ERROR:
                    youtubeDownloader.failDownload();
                    break;
                case DONE:
                    downloadInfo
                            .stream()
                            .map(vi -> vi.targetFile)
                            .filter(Objects::nonNull)
                            .forEach(f -> log.debug("{} - Téléchargement terminé", FilenameUtils.getName(f.getAbsolutePath())));
                    if (item.getStatus() == Status.STARTED)
                        youtubeDownloader.finishDownload();
                    break;
                case RETRYING:
                    log.debug(info.getState() + " " + info.getDelay());
                    if (info.getDelay() == 0) {
                        log.error(info.getException().toString());
                    }
                    if (DownloadIOCodeError.class.isInstance(info.getException())) {
                        log.debug("Cause  : " + DownloadIOCodeError.class.cast(info.getException()).getCode());
                    }

                    if (launchDateDownload.isBefore(now().minusHours(MAX_WAITING_MINUTE))) {
                        youtubeDownloader.failDownload();
                    }
                    break;
                case DOWNLOADING:
                    downloading(downloadInfo, item);
                    break;
                case STOP:
                    log.debug("Pause / Arrêt du téléchargement du téléchargement");
                    break;
                default:
                    break;
            }
        }

        private void downloading(List<VideoFileInfo> downloadInfo, Item item) {

            if (isNull(globalSize)) {
                globalSize = downloadInfo.stream()
                        .filter(v -> nonNull(v.getLength()))
                        .mapToLong(URLInfo::getLength)
                        .sum();
            }

            Long count = downloadInfo.stream().mapToLong(DownloadInfo::getCount).sum();
            int currentState = (int) (count * 100 / (float) globalSize );
            if (item.getProgression() < currentState) {
                item.setProgression(currentState);
                log.debug("{} - {}%", item.getTitle(), item.getProgression());
                youtubeDownloader.convertAndSaveBroadcast();
            }
        }
    }
}
