package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.DateUtils;
import lan.dk.podcastserver.utils.DigestUtils;
import lan.dk.podcastserver.utils.ImageUtils;
import lan.dk.podcastserver.utils.jDomUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Set;

@Component("YoutubeUpdater")
@Scope("prototype")
public class YoutubeUpdater extends AbstractUpdater {

    private static final Integer YOUTUBE_MAX_RESULTS = 50;
    private static final String GDATA_USER_FEED = "http://gdata.youtube.com/feeds/api/users/";
    //private static final String YOUTUBE_VIDEO_URL = "http://www.youtube.com/watch?v=";

    @Value("${numberofdaytodownload:30}") Integer numberOfDayToDownload;

    public Podcast updateFeed(Podcast podcast) {

        Integer borne = 1;
        String realPodcastURl;
        ZonedDateTime maxDate = ZonedDateTime.now().plusDays(numberOfDayToDownload);
        Namespace media = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");

        while (true) {
            // Si l'image de présentation a changé :
            realPodcastURl = this.gdataUrlFromYoutubeURL(podcast.getUrl(), borne);
            logger.debug("URL = {}", realPodcastURl);
            Document podcastXMLSource;
            try {
                podcastXMLSource = jDomUtils.jdom2Parse(realPodcastURl);
                Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();

                if (podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).size() == 0) {
                    return podcast;
                }

                for (Element item : podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace)) {
                    Item podcastItem = new Item()
                            .setTitle(item.getChildText("title", defaultNamespace))
                            .setDescription(item.getChildText("content", defaultNamespace))
                            .setPubdate(DateUtils.fromYoutube(item.getChildText("published", defaultNamespace)))
                            .setPodcast(podcast);

                    if (podcastItem.getPubdate().isBefore(maxDate) && borne > YOUTUBE_MAX_RESULTS) {
                        return podcast;
                    }
                    for (Element link : item.getChildren("link", defaultNamespace)) {
                        if (link.getAttributeValue("rel", null, "").equals("alternate") ) {
                            podcastItem.setUrl(link.getAttributeValue("href", null, ""));
                            break;
                        }
                    }


                    if (!podcast.getItems().contains(podcastItem)) {
                        if (    item.getChild("group", media) != null &&
                                !item.getChild("group", media).getChildren("thumbnail", media).isEmpty() &&
                                item.getChild("group", media).getChildren("thumbnail", media).get(0) != null) {
                            Cover cover = ImageUtils.getCoverFromURL(new URL(item.getChild("group", media).getChildren("thumbnail", media).get(0).getAttributeValue("url")));
                            podcastItem.setCover(cover);
                        }

                        Set<ConstraintViolation<Item>> constraintViolations = validator.validate( podcastItem );
                        if (constraintViolations.isEmpty()) {
                            podcast.getItems().add(podcastItem);
                        } else {
                            logger.error(constraintViolations.toString());
                        }
                    }

                }

            } catch (JDOMException | IOException e) {
                e.printStackTrace();
                return podcast;
            }

            borne += YOUTUBE_MAX_RESULTS;
        }


        //return podcast;
    }

    @Override
    public Podcast findPodcast(String url) {
        return null;
    }

    @Override
    public String signaturePodcast(Podcast podcast) {
        // Si l'image de présentation a changé :
        Document podcastXMLSource;
        try {
            podcastXMLSource = jDomUtils.jdom2Parse(this.gdataUrlFromYoutubeURL(podcast.getUrl(), null));
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return "";
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return "";
        }

        Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();

        if (podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).get(0) != null) {
            return DigestUtils.generateMD5SignatureFromDOM (podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).get(0).getChildText("published", defaultNamespace));
        }
        return "";
    }


    //** Helper Youtube **//
    private String gdataUrlFromYoutubeURL(String youtubeUrl, Integer startIndex) { //
        String queryParam = "?max-results=".concat(String.valueOf(YOUTUBE_MAX_RESULTS))
                .concat((startIndex != null)
                        ? "&start-index=" + startIndex.toString()
                        : "");

        if ( (youtubeUrl.matches(".*.youtube.com/channel/.*") ||
                youtubeUrl.matches(".*.youtube.com/user/.*") ||
                youtubeUrl.matches(".*.youtube.com/.*")) && !youtubeUrl.contains("gdata") ) { //www.youtube.com/[channel|user]*/nom
            return GDATA_USER_FEED + youtubeUrl.substring(youtubeUrl.lastIndexOf("/") + 1) + "/uploads" + queryParam; //http://gdata.youtube.com/feeds/api/users/cauetofficiel/uploads
        } else if (youtubeUrl.matches(".*gdata.youtube.com/feeds/api/playlists/.*")) {
            return youtubeUrl + queryParam;
        }
        return null;

    }


}
