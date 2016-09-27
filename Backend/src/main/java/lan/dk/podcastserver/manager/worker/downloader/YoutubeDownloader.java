package lan.dk.podcastserver.manager.worker.downloader;

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
import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.service.FfmpegService;
import lan.dk.podcastserver.service.factory.WGetFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

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

    @Autowired WGetFactory wGetFactory;
    @Autowired FfmpegService ffmpegService;

    @Override
    public Item download() {
        log.debug("Download");
        try {
            VGetParser parser = wGetFactory.parser(item.getUrl());
            v = wGetFactory.newVGet(parser.info(new URL(item.getUrl())));

            v.extract(parser, stopDownloading, watcher);

            v
                .getVideo()
                .getInfo()
                .forEach(vi -> vi.targetFile = generatePartFile(getTargetFile(item, v.getVideo().getTitle()), vi).toFile());

            v.download(parser, stopDownloading, watcher);
        } catch (DownloadMultipartError e) {
            e.getInfo()
                .getParts()
                .stream()
                .map(DownloadInfo.Part::getException)
                .filter(Objects::nonNull)
                .forEach(Throwable::printStackTrace);

            stopDownload();
        } catch (DownloadInterruptedError e) {
            log.debug("Arrêt du téléchargement par l'interface");
        } catch (StringIndexOutOfBoundsException | MalformedURLException | NullPointerException | DownloadError e) {
            log.error("Third part Exception : ", e);
            if (itemDownloadManager.canBeReset(item)) {
                log.info("Reset of Youtube download {}", item.getTitle());
                itemDownloadManager.resetDownload(item);
                return null;
            }
            stopDownload();
        }
        log.debug("Download ended");
        return item;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private Path generatePartFile(Path targetFile, VideoFileInfo vi) {
        return targetFile.resolveSibling(targetFile.getFileName() + v.getContentExt(vi));
    }

    private Path getTargetFile(Item item, String youtubeTitle) {

        if (nonNull(target)) return target;

        try {
            Path file = podcastServerParameters.getRootfolder().resolve(item.getPodcast().getTitle()).resolve(youtubeTitle.replaceAll("[^a-zA-Z0-9.-]", "_").concat(temporaryExtension));
            if (!Files.exists(file.getParent())) {
                Files.createDirectories(file.getParent());
            }

            Files.deleteIfExists(file);

            target = Files.createFile(file);
            return target;
        } catch (IOException e) {
            throw new RuntimeException("Error during creation of file", e);
        }
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
                .forEach(p -> Try.of(() -> Files.deleteIfExists(p)));
    }

    @Override
    public void finishDownload() {

        try {
            Path fileWithExtension = target.resolveSibling(getDefinitiveFileName());
            Files.deleteIfExists(target);

            if (hasOnlyOneStream()) {
                target = Files.move(v.getVideo().getInfo().get(0).targetFile.toPath(), fileWithExtension, StandardCopyOption.REPLACE_EXISTING);;
            } else {
                Path audioFile = getStream("audio");
                Path video = getStream("video");

                target = ffmpegService.mergeAudioAndVideo(video, audioFile, fileWithExtension);

                Files.deleteIfExists(video);
                Files.deleteIfExists(audioFile);
            }
        } catch (IOException e) {
            log.error("Error during specific move", e);
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
    public Integer compatibility(String url) {
        return url.contains("www.youtube.com") ? 1 : Integer.MAX_VALUE;
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
                    youtubeDownloader.stopDownload();
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
                        youtubeDownloader.stopDownload();
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
