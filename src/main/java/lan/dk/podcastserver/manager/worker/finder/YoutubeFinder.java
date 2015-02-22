package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.xml.JdomService;
import lan.dk.podcastserver.utils.ImageUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * Created by kevin on 22/02/15.
 */
@Service("YoutubeFinder")
public class YoutubeFinder implements Finder {

    public static final Namespace MEDIA_NAMESPACE = Namespace.getNamespace("http://search.yahoo.com/mrss/");
    public static final Namespace XMLNS_NAMESPACE = Namespace.getNamespace("http://www.w3.org/2005/Atom");
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String GDATA_USER_FEED = "https://gdata.youtube.com/feeds/api/users/";
    
    @Resource JdomService jdomService;
    
    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        Podcast youtubePodcst = new Podcast()
                .setUrl(url)
                .setType("Youtube");

        Document document;
        try {
            document = jdomService.jdom2Parse(gdataUrlFromYoutubeURL(url));
        } catch (JDOMException | IOException e) {
            logger.error("Error during parsing of podcast", e);
            throw new FindPodcastNotFoundException();
        }

        Element rootElement = document.getRootElement();
        youtubePodcst
                .setTitle(rootElement.getChildText("title", XMLNS_NAMESPACE))
                .setDescription(rootElement.getChildText("content", XMLNS_NAMESPACE));

        try {
            youtubePodcst.setCover(ImageUtils.getCoverFromURL(rootElement.getChild("thumbnail", MEDIA_NAMESPACE).getAttributeValue("url")));
        } catch (IOException e) {
            logger.error("Can't fetch image from gdata_description", e);
        }


        return youtubePodcst;
    }

    private String gdataUrlFromYoutubeURL(String youtubeUrl) { 

        if ( (youtubeUrl.matches(".*.youtube.com/channel/.*") || youtubeUrl.matches(".*.youtube.com/user/.*") || youtubeUrl.matches(".*.youtube.com/.*")) && !youtubeUrl.contains("gdata") ) { //www.youtube.com/[channel|user]*/nom
            return GDATA_USER_FEED + StringUtils.substringAfterLast(youtubeUrl, "/"); //http://gdata.youtube.com/feeds/api/users/cauetofficiel/uploads
        } else if (youtubeUrl.matches(".*gdata.youtube.com/feeds/api/playlists/.*")) {
            return "https://" + StringUtils.substringAfter(youtubeUrl, "://");
        }
        return null;
    }
}
