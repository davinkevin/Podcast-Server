package lan.dk.podcastserver.manager.worker.downloader;

import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.service.FfmpegService;
import lan.dk.podcastserver.service.M3U8Service;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.factory.ProcessBuilderFactory;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Objects;

import static java.util.Objects.nonNull;

@Scope("prototype")
@Component("M3U8Downloader")
public class M3U8Downloader extends AbstractDownloader {

    @Autowired UrlService urlService;
    @Autowired M3U8Service m3U8Service;
    @Autowired FfmpegService ffmpegService;
    @Autowired ProcessBuilderFactory processBuilderFactory;

    private Process process;

    @Override
    public Item download() {
        logger.debug("Download");

        target = getTargetFile(item);

        Double duration = ffmpegService.getDurationOf(getItemUrl(item), withUserAgent());

        FFmpegBuilder command = new FFmpegBuilder()
                .setUserAgent(withUserAgent())
                .addInput(getItemUrl(item))
                .addOutput(target.toAbsolutePath().toString())
                    .setFormat("mp4")
                    .setAudioBitStreamFilter(FfmpegService.AUDIO_BITSTREAM_FILTER_AAC_ADTSTOASC)
                    .setVideoCodec(FfmpegService.CODEC_COPY)
                    .setAudioCodec(FfmpegService.CODEC_COPY)
                .done();


        process = ffmpegService.download(getItemUrl(item), command, handleProgression(duration));

        Try.run(process::waitFor);

        if (item.getStatus() == Status.STARTED)
            finishDownload();

        return item;
    }

    protected String withUserAgent() {
        return UrlService.USER_AGENT_DESKTOP;
    }

    private ProgressListener handleProgression(Double duration) {
        return p -> broadcastProgression(((Float) (Long.valueOf(p.out_time_ms).floatValue() / duration.floatValue() * 100)).intValue());
    }

    private void broadcastProgression(int cpt) {
        item.setProgression(cpt);
        logger.debug("Progression : {}", item.getProgression());
        convertAndSaveBroadcast();
    }

    @Override
    public Path getTargetFile(Item item) {

        if (nonNull(target))
            return target;

        if (!Objects.equals(item, this.item))
            return super.getTargetFile(item);


        Item m3u8Item = Item.builder()
                .podcast(item.getPodcast())
                .url(FilenameUtils.getBaseName(StringUtils.substringBeforeLast(getItemUrl(item), "?")).concat(".mp4"))
            .build();

        return super.getTargetFile(m3u8Item);
    }

    @Override
    public void pauseDownload() {
        ProcessBuilder pauseProcess = processBuilderFactory.newProcessBuilder("kill", "-STOP", "" + processBuilderFactory.pidOf(process));
        Try
            .run(pauseProcess::start)
            .andThenTry(super::pauseDownload)
            .onFailure(e -> {
                logger.error("Error during pause of process :", e);
                this.stopDownload();
            });
    }

    @Override
    public void restartDownload() {
        ProcessBuilder pauseProcess = processBuilderFactory.newProcessBuilder("kill", "-SIGCONT", "" + processBuilderFactory.pidOf(process));
        Try
            .run(pauseProcess::start)
            .andThenTry(() -> {
                item.setStatus(Status.STARTED);
                saveSyncWithPodcast();
                convertAndSaveBroadcast();
            })
            .onFailure(e -> {
                logger.error("Error during restart of process :", e);
                this.stopDownload();
            });
    }

    @Override
    public void stopDownload() {
        Try.run(() -> process.destroy());
        super.stopDownload();
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("m3u8") ? 10 : Integer.MAX_VALUE;
    }
}
