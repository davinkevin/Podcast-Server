package lan.dk.podcastserver.manager.worker.downloader;

import com.github.axet.wget.WGet;
import com.github.axet.wget.info.DownloadInfo;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.utils.FfmpegUtils;
import lan.dk.podcastserver.utils.URLUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 13/07/2014.
 */
@Component("ParleysDownloader")
@Scope("prototype")
public class ParleysDownloader extends AbstractDownloader{

    public static final String PARLEYS_ITEM_API_URL = "http://api.parleys.com/api/presentation.json/{ID_VIDEO}?view=true";

    /* Patter to extract value from URL */
    // --> http://www.parleys.com/play/535a2846e4b03397a8eee892
    public static Pattern ID_PARLEYS_PATTERN = Pattern.compile(".*/play/([^/]*)");

    protected DownloadInfo info = null;
    private Long totalSize;
    private int avancementIntermediaire = 0;

    @Resource
    FfmpegUtils ffmpegUtils;


    @Override
    public Item download() {

        itemDownloadManager.addACurrentDownload();
        item.setProgression(0);

        List<ParleysAssets> listOfAssets = getUrlForParleysItem(item);
        totalSize = getTotalSize(listOfAssets);

        Runnable itemSynchronisation = new Runnable() {
            long last;

            @Override
            public void run() {
                // notify app or save download state
                // you can extract information from DownloadInfo info;
                switch (info.getState()) {
                    case EXTRACTING:
                    case EXTRACTING_DONE:
                        logger.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " " + info.getState());
                        break;
                    case ERROR:
                        stopDownload();
                        break;
                    case DONE:
                        logger.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " - terminé");
                        avancementIntermediaire = item.getProgression();
                        break;
                    case RETRYING:
                        logger.debug(FilenameUtils.getName(String.valueOf(item.getUrl())) + " " + info.getState() + " " + info.getDelay());
                        break;
                    case DOWNLOADING:
                        long now = System.currentTimeMillis();

                        if (now - 1000 > last && info.getLength() != null && info.getLength() != 0L) {
                            last = now;
                            int progression = ((int) (info.getCount()*100 / (float) totalSize))+avancementIntermediaire;
                            if (item.getProgression() < progression) {
                                item.setProgression(progression);
                                logger.debug("Progression de {} : {}%", item.getTitle(), progression);
                                convertAndSaveBroadcast();
                            }
                        }
                        break;
                    case STOP:
                        logger.debug("Pause / Arrêt du téléchargement du téléchargement");
                        //stopDownload();
                        break;
                    default:
                        break;
                }
            }
        };


        for(ParleysAssets parleysAssets : listOfAssets) {
            try {
                URL url = new URL(parleysAssets.getUrl());
                // initialize url information object
                info = new DownloadInfo(url);
                info.extract(stopDownloading, itemSynchronisation);
                parleysAssets.setFile(getTagetFile(parleysAssets.getUrl()));
                WGet w = new WGet(info, parleysAssets.getFile());
                w.download(stopDownloading, itemSynchronisation);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        target = getTagetFile(item);
        target = new File(target.getParentFile(), FilenameUtils.removeExtension(target.getName()).concat(".mp4"));
        List<File> listOfFilesToConcat = getListOfFiles(listOfAssets);
        logger.info("Finalisation du téléchargement");

        logger.info("Concatenation des vidéos");
        ffmpegUtils.concatDemux(target, listOfFilesToConcat.toArray(new File[listOfFilesToConcat.size()]));

        for(File partFile : listOfFilesToConcat) {
            partFile.delete();
        }

        finishDownload();
        itemDownloadManager.removeACurrentDownload(item);

        return null;
    }

    private List<File> getListOfFiles(List<ParleysAssets> listOfAssets) {
        List<File> listOfFilesToConcat = new ArrayList<>();
        for (ParleysAssets parleysAssets : listOfAssets) {
            listOfFilesToConcat.add(parleysAssets.getFile());
        }
        return listOfFilesToConcat;
    }

    private Long getTotalSize(List<ParleysAssets> listOfAssets) {
        Long total = 0L;
        for(ParleysAssets parleysAssets : listOfAssets) {
            total += parleysAssets.getSize();
        }
        return total;
    }

    private List<ParleysAssets> getUrlForParleysItem(Item item) {
        List<ParleysAssets> listOfAssets = new ArrayList<>();

        try {
            JSONObject itemJsonObject = getParseJsonObjectForItem(item.getUrl());
            JSONArray arrayOfAssets = (JSONArray) itemJsonObject.get("assets");

            for (Object assetObject : arrayOfAssets) {
                JSONObject asset = (JSONObject) assetObject;


                if ("STREAM".equals(asset.get("target"))) {
                    ParleysAssets parleysAssets = getParleysAssets("MP4", (JSONArray) asset.get("files"));
                    if (parleysAssets != null) {
                        listOfAssets.add(parleysAssets);
                    }

                }
            }

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }


        return listOfAssets;
    }

    private ParleysAssets getParleysAssets(String type, JSONArray arrayOfParleysFiles) {

        for (Object fileObject : arrayOfParleysFiles) {
            JSONObject file = (JSONObject) fileObject;

            if (type.equals(file.get("format"))) {
                return new ParleysAssets()
                        .setUrl((String) file.get("httpDownloadURL"))
                        .setSize((Long) file.get("fileSize"));
            }

        }

        return null;
    }


    private String getItemUrl(String id) {
        return PARLEYS_ITEM_API_URL.replace("{ID_VIDEO}", id);
    }

    private String getParleysId(String url) {
        // Extraction de l'id de l'emission :
        Matcher m = ID_PARLEYS_PATTERN.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private JSONObject getParseJsonObjectForItem(String url) throws IOException, ParseException {
        return (JSONObject) new JSONParser().parse(URLUtils.getReaderFromURL(getItemUrl(getParleysId(url))));
    }

    private class ParleysAssets {
        private String url;
        private Long size;
        private File file;

        public File getFile() {
            return file;
        }

        public ParleysAssets setFile(File file) {
            this.file = file;
            return this;
        }

        public String getUrl() {
            return url;
        }

        public ParleysAssets setUrl(String url) {
            this.url = url;
            return this;
        }

        public Long getSize() {
            return size;
        }

        public ParleysAssets setSize(Long size) {
            this.size = size;
            return this;
        }
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
}
