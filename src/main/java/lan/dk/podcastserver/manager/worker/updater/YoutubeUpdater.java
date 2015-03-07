package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.xml.JdomService;
import lan.dk.podcastserver.utils.ImageUtils;
import lan.dk.podcastserver.utils.URLUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Scope("prototype")
@Component("YoutubeUpdater")
public class YoutubeUpdater extends AbstractUpdater {

    private static final Integer YOUTUBE_MAX_RESULTS = 50;
    private static final String GDATA_USER_FEED = "https://gdata.youtube.com/feeds/api/users/";
    //private static final String YOUTUBE_VIDEO_URL = "http://www.youtube.com/watch?v=";

    @Resource JdomService jdomService;
    
    public static ZonedDateTime fromYoutube(String pubDate) {
        return ZonedDateTime.parse(pubDate, DateTimeFormatter.ISO_DATE_TIME); //2013-12-20T22:30:01.000Z
    }

    public Podcast updateAndAddItems(Podcast podcast) {

        getItems(podcast).stream()
                .filter(notIn(podcast))
                .map(item -> item.setPodcast(podcast))
                .filter(item -> validator.validate(item).isEmpty())
                .forEach(podcast::add);
        
        return podcast;
    }

    public Set<Item> getItems(Podcast podcast) {
        Set<Item> itemSet = new HashSet<>();

        Integer borne = 1;
        String realPodcastURl;
        ZonedDateTime maxDate = ZonedDateTime.now().plusDays(podcastServerParameters.numberOfDayToDownload());
        Namespace media = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");

        while (true) {
            // Si l'image de présentation a changé :
            realPodcastURl = this.gdataUrlFromYoutubeURL(podcast.getUrl(), borne);
            logger.debug("URL = {}", realPodcastURl);
            Document podcastXMLSource;
            try {
                podcastXMLSource = jdomService.jdom2Parse(realPodcastURl);
                Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();

                if (podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).size() == 0) {
                    return itemSet;
                }

                for (Element item : podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace)) {
                    Item podcastItem = new Item()
                            .setTitle(item.getChildText("title", defaultNamespace))
                            .setDescription(item.getChildText("content", defaultNamespace))
                            .setPubdate(fromYoutube(item.getChildText("published", defaultNamespace)))
                            .setPodcast(podcast);

                    if (podcastItem.getPubdate().isBefore(maxDate) && borne > YOUTUBE_MAX_RESULTS) {
                        return itemSet;
                    }
                    for (Element link : item.getChildren("link", defaultNamespace)) {
                        if (link.getAttributeValue("rel", null, "").equals("alternate") ) {
                            podcastItem.setUrl(URLUtils.changeProtocol(link.getAttributeValue("href", null, ""), "http", "https"));
                            break;
                        }
                    }

                    if (    item.getChild("group", media) != null &&
                            !item.getChild("group", media).getChildren("thumbnail", media).isEmpty() &&
                            item.getChild("group", media).getChildren("thumbnail", media).get(0) != null) {
                        Cover cover = ImageUtils.getCoverFromURL(new URL(item.getChild("group", media).getChildren("thumbnail", media).get(0).getAttributeValue("url")));
                        podcastItem.setCover(cover);
                    }

                    itemSet.add(podcastItem);

                }

            } catch (JDOMException | IOException e) {
                e.printStackTrace();
                return itemSet;
            }

            borne += YOUTUBE_MAX_RESULTS;
        }

    }

    @Override
    public Podcast findPodcast(String url) {
        return null;
    }

    @Override
    public String generateSignature(Podcast podcast) {
        // Si l'image de présentation a changé :
        Document podcastXMLSource;
        try {
            podcastXMLSource = jdomService.jdom2Parse(this.gdataUrlFromYoutubeURL(podcast.getUrl(), null));
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return "";
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return "";
        }

        Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();

        if (podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).get(0) != null) {
            return signatureService.generateMD5SignatureFromDOM(podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).get(0).getChildText("published", defaultNamespace));
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
            return "https://" + StringUtils.substringAfter(youtubeUrl + queryParam, "://");
        }
        return null;

    }


}
