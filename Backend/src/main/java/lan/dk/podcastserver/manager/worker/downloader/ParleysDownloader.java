package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.info.DownloadInfo;
import com.google.common.collect.Lists;
import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.FfmpegService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.factory.WGetFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

/**
 * Created by kevin on 13/07/2014 for Podcast Server
 */
@Scope("prototype")
@Component("ParleysDownloader")
public class ParleysDownloader extends AbstractDownloader{

    private static final String PARLEYS_ITEM_API_URL = "http://api.parleys.com/api/presentation.json/%s?view=true";
    private static final String STREAM_TOKEN = "STREAM";

    /* Patter to extract value from URL */
    // --> http://www.parleys.com/play/535a2846e4b03397a8eee892
    private static Pattern ID_PARLEYS_PATTERN = Pattern.compile(".*/play/([^/]*)");

    protected DownloadInfo info = null;
    private Long totalSize;
    private Path podcastPath;

    @Autowired FfmpegService ffmpegService;
    @Autowired UrlService urlService;
    @Autowired WGetFactory wGetFactory;
    @Autowired JsonService jsonService;

    @Override
    public Item download() {

        item.setProgression(0);

        List<ParleysAssets> listOfAssets = getUrlForParleysItem(item);
        if ((listOfAssets.size() == 0)) {
            logger.error("No assets found for the item with url {}", item.getUrl());
            stopDownload();
            return null;
        }

        totalSize = getTotalSize(listOfAssets);

        Runnable itemSynchronisation = new ParleysWatcher(this);

        for(ParleysAssets parleysAssets : listOfAssets) {
            try {
                info = wGetFactory.newDownloadInfo(parleysAssets.getUrl());
                info.extract(stopDownloading, itemSynchronisation);

                wGetFactory
                        .newWGet(info, parleysAssets.getFile().toFile())
                        .download(stopDownloading, itemSynchronisation);
            } catch (MalformedURLException e) {
                logger.error("Url of assest is invalid : {}", parleysAssets.getUrl(), e);
                parleysAssets.setValid(Boolean.FALSE);
            }
        }

        target = getTargetFile(item);
        List<Path> listOfFilesToConcat = getListOfFiles(listOfAssets);
        logger.info("Finalisation du téléchargement");

        logger.info("Concatenation des vidéos");
        ffmpegService.concat(target, listOfFilesToConcat.toArray(new Path[listOfFilesToConcat.size()]));

        listOfFilesToConcat
                .forEach(f -> Try.run(() -> Files.deleteIfExists(f)));

        finishDownload();
        itemDownloadManager.removeACurrentDownload(item);

        return null;
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("parleys") ? 1 : Integer.MAX_VALUE;
    }

    private List<Path> getListOfFiles(List<ParleysAssets> listOfAssets) {
        return listOfAssets.stream().filter(ParleysAssets::getValid).map(ParleysAssets::getFile).collect(toList());
    }

    private Long getTotalSize(List<ParleysAssets> listOfAssets) {
        return listOfAssets.stream().mapToLong(ParleysAssets::getSize).sum();
    }

    private List<ParleysAssets> getUrlForParleysItem(Item item) {
        return getParseJsonObjectForItem(item.getUrl())
                .map(o -> JSONArray.class.cast(o.get("assets")))
                .map(this::formJsonArrayToList)
                .map(list -> list.stream().map(a -> a.setFile(getAssetFile(a.getUrl()))).collect(toList()))
                .orElse(Lists.newArrayList());
    }

    @SuppressWarnings("unchecked")
    private List<ParleysAssets> formJsonArrayToList(JSONArray array) {
        return ((List<JSONObject>) array)
                .stream()
                .filter(i -> STREAM_TOKEN.equals(i.get("target")))
                .map(i -> getParleysAssets("MP4", (JSONArray) i.get("files")))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    @SuppressWarnings("unchecked")
    private ParleysAssets getParleysAssets(String type, JSONArray arrayOfParleysFiles) {
        return ((List<JSONObject>) arrayOfParleysFiles)
                .stream()
                .filter(f -> type.equals(f.get("format")))
                .map(i -> ParleysAssets.builder().url((String) i.get("httpDownloadURL")).size((Long) i.get("fileSize")).valid(Boolean.TRUE).build())
                .findFirst()
                .orElse(null);
    }


    private String getItemUrl(String id) {
        return String.format(PARLEYS_ITEM_API_URL, id);
    }

    private Optional<String> getParleysId(String url) {
        // Extraction de l'id de l'emission :
        Matcher m = ID_PARLEYS_PATTERN.matcher(url);
        if (m.find()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

    private Optional<JSONObject> getParseJsonObjectForItem(String url) {
        return getParleysId(url)
                .map(this::getItemUrl)
                .flatMap(urlService::newURL)
                .flatMap(jsonService::from);
    }


    @Builder
    @Getter @Setter
    @Accessors(chain = true)
    private static class ParleysAssets {
        String url;
        Long size;
        Path file;
        Boolean valid;
    }

    @Override
    public Path getTargetFile(Item item) {

        if (nonNull(target)) return target;

        Path targetFile = super.getTargetFile(item);

        return targetFile.resolveSibling(FilenameUtils.getBaseName(targetFile.getFileName().toString()) + ".mp4");
    }

    private Path getAssetFile(String url) {

        if (isNull(podcastPath)) {
            podcastPath = Paths.get(itemDownloadManager.getRootfolder(), item.getPodcast().getTitle());
            try { Files.createDirectory(podcastPath); } catch (IOException ignored) {}
        }

        String urlWithoutParameters = StringUtils.substringBeforeLast(url, "?");

        Path finalFile = podcastPath.resolve(FilenameUtils.getName(urlWithoutParameters));
        try { Files.deleteIfExists(finalFile); } catch (IOException ignored) {}

        return finalFile;
    }

    @Slf4j
    static class ParleysWatcher implements Runnable {

        private final ParleysDownloader parleysDownloader;
        private Integer avancementIntermediaire = 0;

        public ParleysWatcher(ParleysDownloader parleysDownloader) {
            this.parleysDownloader = parleysDownloader;
        }

        @Override
        public void run() {
            Item item = parleysDownloader.item;
            DownloadInfo info = parleysDownloader.info;

            switch (info.getState()) {
                case EXTRACTING:
                case EXTRACTING_DONE:
                    log.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " " + info.getState());
                    break;
                case ERROR:
                    parleysDownloader.stopDownload();
                    break;
                case DONE:
                    log.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " - terminé");
                    avancementIntermediaire = item.getProgression();
                    break;
                case RETRYING:
                    log.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " " + info.getState() + " " + info.getDelay());
                    break;
                case DOWNLOADING:
                    if (isNull(info.getLength()) || ( nonNull(info.getLength()) && info.getCount() == 0L) ) break;

                    int progression = ((int) ((info.getCount() * 100) / (float) parleysDownloader.totalSize))+avancementIntermediaire;
                    if (item.getProgression() < progression) {
                        item.setProgression(progression);
                        log.debug("Progression de {} : {}%", item.getTitle(), progression);
                        parleysDownloader.convertAndSaveBroadcast();
                    }

                    break;
                case STOP:
                    log.debug("Pause / Arrêt du téléchargement du téléchargement");
                    break;
                default:
                    break;
            }
        }
    }
}
