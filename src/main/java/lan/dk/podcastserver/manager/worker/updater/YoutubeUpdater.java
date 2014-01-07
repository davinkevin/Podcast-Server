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
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;

/**
 * Created by kevin on 21/12/2013.
 */
@Component("YoutubeUpdater")
@Scope("prototype")
public class YoutubeUpdater extends AbstractUpdater {

    private String GDATA_USER_FEED = "http://gdata.youtube.com/feeds/api/users/";
    private String YOUTUBE_VIDEO_URL = "http://www.youtube.com/watch?v=";

    @Override
    public Podcast updateFeed(Podcast podcast) {

        String realPodcastURl = this.gdataUrlFromYoutubeURL(podcast.getUrl());
        logger.debug("URL = {}", realPodcastURl);

        // Si l'image de présentation a changé :
        Document podcastXMLSource = null;
        try {
            podcastXMLSource = jDomUtils.jdom2Parse(realPodcastURl);
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }

        Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();
        Namespace media = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
        for (Element item : podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace)) {
            logger.debug("Entry : {}", item.getChildText("title", defaultNamespace));
            try {
                Item podcastItem = new Item()
                                            .setTitle(item.getChildText("title", defaultNamespace))
                                            .setDescription(item.getChildText("content", defaultNamespace))
                                            .setPubdate(DateUtils.youtubeDateToTimeStamp(item.getChildText("published", defaultNamespace)))
                                            .setPodcast(podcast)
                                            .setUrl(YOUTUBE_VIDEO_URL + item.getChildText("id", defaultNamespace).substring(item.getChildText("id", defaultNamespace).lastIndexOf("/")+1));

                if (!podcast.getItems().contains(podcastItem)) {
                    if (    item.getChild("group", media) != null &&
                            !item.getChild("group", media).getChildren("thumbnail", media).isEmpty() &&
                            item.getChild("group", media).getChildren("thumbnail", media).get(0) != null) {
                        Cover cover = ImageUtils.getCoverFromURL(new URL(item.getChild("group", media).getChildren("thumbnail", media).get(0).getAttributeValue("url")));
                        podcastItem.setCover(cover);
                    }
                    podcast.getItems().add(podcastItem);
                }



            } catch (ParseException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return podcast;
    }

    @Override
    public Podcast findPodcast(String url) {
        return null;
    }

    @Override
    public String signaturePodcast(Podcast podcast) {
        // Si l'image de présentation a changé :
        Document podcastXMLSource = null;
        try {
            podcastXMLSource = jDomUtils.jdom2Parse(this.gdataUrlFromYoutubeURL(podcast.getUrl()));
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }

        Namespace defaultNamespace = podcastXMLSource.getRootElement().getNamespace();
        Namespace media = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");

        if (podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).get(0) != null) {
            return DigestUtils.generateMD5SignatureFromDOM (podcastXMLSource.getRootElement().getChildren("entry", defaultNamespace).get(0).getChildText("published", defaultNamespace));
        }
        return "";
    }


    //** Helper Youtube **//
    private String gdataUrlFromYoutubeURL(String youtubeUrl) { //
        if ( youtubeUrl.matches(".*www.youtube.com/channel/.*") ||
                youtubeUrl.matches(".*www.youtube.com/user/.*") ||
                youtubeUrl.matches(".*www.youtube.com/.*") ) { //www.youtube.com/[channel|user]*/nom
            return GDATA_USER_FEED + youtubeUrl.substring(youtubeUrl.lastIndexOf("/") + 1) + "/uploads?max-results=50"; //http://gdata.youtube.com/feeds/api/users/cauetofficiel/uploads
        } else if (youtubeUrl.matches(".*gdata.youtube.com/feeds/api/playlists/.*")) {
            return youtubeUrl + "?max-results=50";
        }
        return null;

    }


}
