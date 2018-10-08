package lan.dk.podcastserver.manager.downloader;


import com.github.davinkevin.podcastserver.service.MimeTypeService;
import com.github.davinkevin.podcastserver.service.ProcessService;
import com.github.davinkevin.podcastserver.service.UrlService;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import com.github.davinkevin.podcastserver.service.FfmpegService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.vavr.API.Option;
import static io.vavr.API.Try;

@Slf4j
@Scope("prototype")
@Component("FfmpegDownloader")
public class FfmpegDownloader extends AbstractDownloader {

    private final FfmpegService ffmpegService;
    private final ProcessService processService;

    private Process process;
    private Double globalDuration = 0d;
    private Double alreadyDoneDuration = 0d;

    public FfmpegDownloader(ItemRepository itemRepository, PodcastRepository podcastRepository, PodcastServerParameters podcastServerParameters, SimpMessagingTemplate template, MimeTypeService mimeTypeService, FfmpegService ffmpegService, ProcessService processService) {
        super(itemRepository, podcastRepository, podcastServerParameters, template, mimeTypeService);
        this.ffmpegService = ffmpegService;
        this.processService = processService;
    }

    @Override
    public Item download() {
        log.debug("Download {}", item.getTitle());

        target = getTargetFile(this.downloadingItem.getItem());

        globalDuration = this.downloadingItem.getUrls()
                .map(url -> ffmpegService.getDurationOf(url, downloadingItem.getUserAgent()))
                .sum()
                .doubleValue();

        List<Path> multiFiles = this.downloadingItem.getUrls().map(this::download);

        ffmpegService.concat(target, multiFiles.toJavaArray(Path.class));

        multiFiles.forEach(v -> Try.run(() -> Files.delete(v)));

        if (item.getStatus() == Status.STARTED)
            finishDownload();

        return this.downloadingItem.getItem();

    }

    private Path download(String url) {
        Double duration = ffmpegService.getDurationOf(url, downloadingItem.getUserAgent());

        Path subTarget = this.generateTempFileNextTo(target);

        FFmpegBuilder command = new FFmpegBuilder()
                .setUserAgent(Option(downloadingItem.getUserAgent()).getOrElse(UrlService.USER_AGENT_DESKTOP))
                .addInput(url)
                .addOutput(subTarget.toAbsolutePath().toString())
                .setFormat("mp4")
                .setAudioBitStreamFilter(FfmpegService.Companion.getAUDIO_BITSTREAM_FILTER_AAC_ADTSTOASC())
                .setVideoCodec(FfmpegService.Companion.getCODEC_COPY())
                .setAudioCodec(FfmpegService.Companion.getCODEC_COPY())
                .done();


        process = ffmpegService.download(url, command, handleProgression(alreadyDoneDuration, globalDuration));

        processService.waitFor(process);

        alreadyDoneDuration += duration;

        return subTarget;
    }

    private ProgressListener handleProgression(Double alreadyDoneDuration, Double globalDuration) {
        return p -> broadcastProgression(((Float) ((Long.valueOf(p.out_time_ms).floatValue() + alreadyDoneDuration.longValue()) / globalDuration.floatValue() * 100)).intValue());
    }

    private void broadcastProgression(int cpt) {
        item.setProgression(cpt);
        log.debug("Progression : {}", item.getProgression());
        convertAndSaveBroadcast();
    }

    @Override
    public void pauseDownload() {
        ProcessBuilder pauseProcess = processService.newProcessBuilder("kill", "-STOP", "" + processService.pidOf(process));
        Try(() -> processService.start(pauseProcess))
                .andThenTry(super::pauseDownload)
                .onFailure(e -> {
                    log.error("Error during pause of process :", e);
                    this.failDownload();
                });
    }

    @Override
    public void restartDownload() {
        ProcessBuilder restart = new ProcessBuilder("kill", "-SIGCONT", "" + processService.pidOf(process));

        Try(() -> processService.start(restart))
                .andThenTry(() -> {
                    item.setStatus(Status.STARTED);
                    saveSyncWithPodcast();
                    convertAndSaveBroadcast();
                })
                .onFailure(e -> {
                    log.error("Error during restart of process :", e);
                    this.failDownload();
                });
    }

    @Override
    public void stopDownload() {
        Try.run(() -> process.destroy());
        super.stopDownload();
    }

    @Override
    public Integer compatibility(DownloadingItem downloadingItem) {
        return downloadingItem.getUrls().forAll(url -> url.contains("m3u8") || url.contains("mp4"))
                ? 10
                : Integer.MAX_VALUE;
    }
}
