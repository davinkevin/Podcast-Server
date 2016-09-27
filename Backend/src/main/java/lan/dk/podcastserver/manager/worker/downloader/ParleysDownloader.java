package lan.dk.podcastserver.manager.worker.downloader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.axet.wget.info.DownloadInfo;
import com.google.common.collect.Lists;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.TypeRef;
import javaslang.control.Option;
import javaslang.control.Try;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.FfmpegService;
import lan.dk.podcastserver.service.JsonService;
import lan.dk.podcastserver.service.factory.WGetFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;

/**
 * Created by kevin on 13/07/2014 for Podcast Server
 */
@Slf4j
@Scope("prototype")
@Component("ParleysDownloader")
public class ParleysDownloader extends AbstractDownloader{

    private static final String PARLEYS_ITEM_API_URL = "http://api.parleys.com/api/presentation.json/%s?view=true";
    private static final String STREAM_TOKEN = "STREAM";
    private static final TypeRef<List<ParleysAssetsDetail>> LIST_PARLEUS_ASSETS_DETAIL_TYPE = new TypeRef<List<ParleysAssetsDetail>>(){};
    /* Patter to extract value from URL */
    // --> http://www.parleys.com/play/535a2846e4b03397a8eee892
    private static Pattern ID_PARLEYS_PATTERN = Pattern.compile(".*/play/([^/]*)");

    protected DownloadInfo info = null;
    private Long totalSize;
    private Path podcastPath;

    @Autowired FfmpegService ffmpegService;
    @Autowired WGetFactory wGetFactory;
    @Autowired JsonService jsonService;

    @Override
    public Item download() {

        item.setProgression(0);

        List<ParleysAssetsDetail.ParleysAssetsFiles> listOfAssets = getUrlForParleysItem(item);
        if ((listOfAssets.size() == 0)) {
            log.error("No assets found for the item with url {}", item.getUrl());
            stopDownload();
            return null;
        }

        totalSize = getTotalSize(listOfAssets);

        Runnable itemSynchronisation = new ParleysWatcher(this);

        for(ParleysAssetsDetail.ParleysAssetsFiles parleysAssets : listOfAssets) {
            try {
                info = wGetFactory.newDownloadInfo(parleysAssets.getUrl());
                info.extract(stopDownloading, itemSynchronisation);

                wGetFactory
                        .newWGet(info, parleysAssets.getFile().toFile())
                        .download(stopDownloading, itemSynchronisation);
            } catch (MalformedURLException e) {
                log.error("Url of assest is invalid : {}", parleysAssets.getUrl(), e);
                parleysAssets.setValid(Boolean.FALSE);
            }
        }

        target = getTargetFile(item);
        List<Path> listOfFilesToConcat = getListOfFiles(listOfAssets);
        log.info("Finalisation du téléchargement");

        log.info("Concatenation des vidéos");
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

    private List<Path> getListOfFiles(List<ParleysAssetsDetail.ParleysAssetsFiles> listOfAssets) {
        return listOfAssets.stream().filter(ParleysAssetsDetail.ParleysAssetsFiles::getValid).map(ParleysAssetsDetail.ParleysAssetsFiles::getFile).collect(toList());
    }

    private Long getTotalSize(List<ParleysAssetsDetail.ParleysAssetsFiles> listOfAssets) {
        return listOfAssets.stream().mapToLong(ParleysAssetsDetail.ParleysAssetsFiles::getFileSize).sum();
    }

    private List<ParleysAssetsDetail.ParleysAssetsFiles> getUrlForParleysItem(Item item) {
        return getParseJsonObjectForItem(item.getUrl())
                .map(p -> p.read("assets", LIST_PARLEUS_ASSETS_DETAIL_TYPE))
                .map(this::assetsToParleysFiles)
                .map(list -> list.stream().map(a -> a.setFile(getAssetFile(a.getUrl()))).collect(toList()))
                .getOrElse(Lists::newArrayList);
    }

    private List<ParleysAssetsDetail.ParleysAssetsFiles> assetsToParleysFiles(List<ParleysAssetsDetail> assetsDetails) {
        return assetsDetails
                .stream()
                .filter(i -> STREAM_TOKEN.equals(i.getTarget()))
                .flatMap( i -> i.getFiles().stream())
                .filter(f -> "MP4".equals(f.getFormat()))
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private String getItemUrl(String id) {
        return String.format(PARLEYS_ITEM_API_URL, id);
    }

    private Option<String> getParleysId(String url) {
        // Extraction de l'id de l'emission :
        Matcher m = ID_PARLEYS_PATTERN.matcher(url);

        if (!m.find())
            return Option.none();

        return Option.of(m.group(1));
    }

    private Option<DocumentContext> getParseJsonObjectForItem(String url) {
        return getParleysId(url)
                .map(this::getItemUrl)
                .flatMap(jsonService::parseUrl);
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ParleysAssetsDetail {
        @Getter @Setter @JsonProperty("target") private String target;
        @Getter @Setter @JsonProperty("files") private List<ParleysAssetsFiles> files;

        @Accessors(chain = true)
        @JsonIgnoreProperties(value = {"file", "valid"}, ignoreUnknown = true)
        private static class ParleysAssetsFiles {
            @JsonProperty("httpDownloadURL") @Setter @Getter private String url;
            @Setter @Getter private Long fileSize;
            @Setter @Getter private String format;
            @Setter @Getter Path file;
            @Setter @Getter Boolean valid = Boolean.TRUE;
        }
    }

    @Override
    public Path getTargetFile(Item item) {

        if (nonNull(target)) return target;

        Path targetFile = super.getTargetFile(item);

        return targetFile.resolveSibling(FilenameUtils.getBaseName(targetFile.getFileName().toString()) + ".mp4");
    }

    private Path getAssetFile(String url) {

        if (isNull(podcastPath)) {
            podcastPath = podcastServerParameters.getRootfolder().resolve(item.getPodcast().getTitle());
            Try.of(() -> Files.createDirectory(podcastPath));
        }

        String urlWithoutParameters = StringUtils.substringBeforeLast(url, "?");

        Path finalFile = podcastPath.resolve(FilenameUtils.getName(urlWithoutParameters));
        Try.of(() -> Files.deleteIfExists(finalFile));

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
