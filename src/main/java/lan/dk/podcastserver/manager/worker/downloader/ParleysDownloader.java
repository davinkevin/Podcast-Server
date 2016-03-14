package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.info.DownloadInfo;
import com.google.common.collect.Lists;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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

    public static final String PARLEYS_ITEM_API_URL = "http://api.parleys.com/api/presentation.json/%s?view=true";
    private static final String STREAM_TOKEN = "STREAM";

    /* Patter to extract value from URL */
    // --> http://www.parleys.com/play/535a2846e4b03397a8eee892
    public static Pattern ID_PARLEYS_PATTERN = Pattern.compile(".*/play/([^/]*)");

    protected DownloadInfo info = null;
    private Long totalSize;

    @Autowired FfmpegService ffmpegService;
    @Autowired UrlService urlService;
    @Autowired WGetFactory wGetFactory;
    @Autowired JsonService jsonService;

    @Override
    public Item download() {

        itemDownloadManager.addACurrentDownload();
        item.setProgression(0);

        List<ParleysAssets> listOfAssets = getUrlForParleysItem(item);
        totalSize = getTotalSize(listOfAssets);

        Runnable itemSynchronisation = new ParleysWatcher(this);

        for(ParleysAssets parleysAssets : listOfAssets) {
            try {
                info = wGetFactory.newDownloadInfo(parleysAssets.getUrl());
                info.extract(stopDownloading, itemSynchronisation);

                parleysAssets.setFile(getTagetFile(parleysAssets.getUrl()));

                wGetFactory
                        .newWGet(info, parleysAssets.getFile())
                        .download(stopDownloading, itemSynchronisation);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        target = getTagetFile(item);
        target = new File(target.getParentFile(), FilenameUtils.removeExtension(target.getName()).concat(".mp4"));
        List<File> listOfFilesToConcat = getListOfFiles(listOfAssets);
        logger.info("Finalisation du téléchargement");

        logger.info("Concatenation des vidéos");
        ffmpegService.concatDemux(target, listOfFilesToConcat.toArray(new File[listOfFilesToConcat.size()]));

        listOfFilesToConcat.forEach(File::delete);

        finishDownload();
        itemDownloadManager.removeACurrentDownload(item);

        return null;
    }

    @Override
    public Integer compatibility(String url) {
        return url.contains("parleys") ? 1 : Integer.MAX_VALUE;
    }

    private List<File> getListOfFiles(List<ParleysAssets> listOfAssets) {
        return listOfAssets.stream().map(ParleysAssets::getFile).collect(toList());
    }

    private Long getTotalSize(List<ParleysAssets> listOfAssets) {
        return listOfAssets.stream().mapToLong(ParleysAssets::getSize).sum();
    }

    private List<ParleysAssets> getUrlForParleysItem(Item item) {
        return getParseJsonObjectForItem(item.getUrl())
                .map(o -> JSONArray.class.cast(o.get("assets")))
                .map(formJsonArrayToList())
                .orElse(Lists.newArrayList());
    }

    @SuppressWarnings("unchecked")
    private Function<JSONArray, List<ParleysAssets>> formJsonArrayToList() {
        return array -> ((List<JSONObject>) array)
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
                .map(i -> ParleysAssets.builder().url((String) i.get("httpDownloadURL")).size((Long) i.get("fileSize")).build())
                .findFirst()
                .get();
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
        File file;
    }

    public File getTagetFile (String url) {

        String urlWithoutParameters = url.substring(0, url.indexOf("?"));

        if (target != null)
            return target;

        File finalFile = new File(itemDownloadManager.getRootfolder() + File.separator + item.getPodcast().getTitle() + File.separator + FilenameUtils.getName(urlWithoutParameters) );
        logger.debug("Création du fichier : {}", finalFile.getAbsolutePath());
        //logger.debug(file.getAbsolutePath());

        if (!finalFile.getParentFile().exists()) {
            finalFile.getParentFile().mkdirs();
        }

        if (finalFile.exists() || new File(finalFile.getAbsolutePath().concat(temporaryExtension)).exists()) {
            logger.info("Doublon sur le fichier en lien avec {} - {}, {}", item.getPodcast().getTitle(), item.getId(), item.getTitle() );
            try {
                finalFile  = File.createTempFile(FilenameUtils.getBaseName(urlWithoutParameters).concat("-"), ".".concat(FilenameUtils.getExtension(urlWithoutParameters)),
                        finalFile.getParentFile());
                finalFile.delete();
            } catch (IOException e) {
                logger.error("Erreur lors du renommage d'un doublon", e);
            }
        }

        return new File(finalFile.getAbsolutePath() + temporaryExtension) ;
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
                    if (isNull(info.getLength()) || (nonNull(info.getLength()) && info.getLength() != 0L)) break;

                    int progression = ((int) (info.getCount()*100 / (float) parleysDownloader.totalSize))+avancementIntermediaire;
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
