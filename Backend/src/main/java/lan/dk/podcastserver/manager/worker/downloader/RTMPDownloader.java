package lan.dk.podcastserver.manager.worker.downloader;


import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.ProcessService;
import lan.dk.podcastserver.service.properties.ExternalTools;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.vavr.API.Try;
import static java.util.Objects.nonNull;

@Slf4j
@Component("RTMPDownloader")
@Scope("prototype")
public class RTMPDownloader extends AbstractDownloader {

    private final ProcessService processService;
    private final ExternalTools externalTools;

    int pid = 0;
    Process p = null;

    public RTMPDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, ProcessService processService, ExternalTools externalTools) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService);
        this.processService = processService;
        this.externalTools = externalTools;
    }

    @Override
    public Item download() {
        log.debug("Download");

        try {
            target = getTargetFile(item);
            log.debug("Fichier de sortie : {}" , target.toAbsolutePath().toString());

            p  = processService
                    .newProcessBuilder(externalTools.getRtmpdump(), "-r", getItemUrl(item), "-o", target.toAbsolutePath().toString())
                    .directory(new File("/tmp"))
                    .redirectErrorStream(true)
                    .start();

            pid = processService.pidOf(p);

            new RTMPWatcher(this).run();

            p.waitFor();
            pid = 0;
        } catch (IOException | InterruptedException e) {
            log.error("IOException | InterruptedException :", e);
            stopDownload();
        }
        return item;
    }

    @Override
    public void startDownload() {
        if (pid != 0 && nonNull(p)) { //Relancement du process UNIX
            log.debug("Stop previous process");
            p.destroy();
        }
        super.startDownload();
    }

    @Override
    public void pauseDownload() {
        ProcessBuilder stopProcess = processService.newProcessBuilder("kill", "-STOP", "" + pid);
        Try(stopProcess::start)
            .andThen(super::pauseDownload)
            .onFailure(e -> {
                log.error("IOException :", e);
                this.stopDownload();
            });
    }

    @Override
    public void stopDownload() {
        if (nonNull(p)) {
            p.destroy();
        }
        super.stopDownload();
    }

    @Override
    public Integer compatibility(String url) {
        return url.startsWith("rtmp://") ? 1 : Integer.MAX_VALUE;
    }

    @Slf4j
    @RequiredArgsConstructor
    static class RTMPWatcher implements Runnable {

        static final String DOWNLOAD_COMPLETE = "download complete";
        static final Pattern RTMPDUMP_PROGRESSION_PATTERN_EXTRACTOR = Pattern.compile("[^(]*\\(([0-9]*).*%\\)");
        final RTMPDownloader rtmpDownloader;

        private BufferedReader getBufferedReader(InputStream is) {
            return new BufferedReader(new InputStreamReader(is));
        }

        @Override
        public void run() {
            Process p = rtmpDownloader.p;
            Item item = rtmpDownloader.item;
            Integer pid = rtmpDownloader.pid;

            BufferedReader br = getBufferedReader(p.getInputStream());
            String line;
            log.debug("Reading output of RTMPDump");
            try {
                while ((line = br.readLine()) != null) {
                    log.debug(line);
                    Matcher m = RTMPDUMP_PROGRESSION_PATTERN_EXTRACTOR.matcher(line);
                    if (progressionHasChange(item, m)) {
                        item.setProgression(Integer.parseInt(m.group(1)));
                        rtmpDownloader.convertAndSaveBroadcast();
                    } else if (isDownloadComplete(line)) {
                        log.info("End of download");
                        rtmpDownloader.finishDownload();
                        break;
                    }
                    if (pid == 0) {
                        break;
                    }
                }
            } catch (IOException e) {
                log.error("IOException :", e);
            }
            if (Status.FINISH != item.getStatus() && !rtmpDownloader.stopDownloading.get()) {
                log.debug("Unexpected ending, reset of downloader");
                rtmpDownloader.resetDownload();
            }
        }

        private boolean isDownloadComplete(String ligne) {
            return ligne.toLowerCase().contains(DOWNLOAD_COMPLETE);
        }

        private boolean progressionHasChange(Item item, Matcher m) {
            return m.matches() && Integer.parseInt(m.group(1)) > item.getProgression();
        }
    }
}
