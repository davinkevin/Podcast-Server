package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.ImageService;
import lan.dk.podcastserver.service.JdomService;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by kevin on 22/02/15.
 */
@Service("RSSFinder")
public class RSSFinder implements Finder {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    JdomService jdomService;
    ImageService imageService;

    @Autowired
    public RSSFinder(JdomService jdomService, ImageService imageService) {
        this.jdomService = jdomService;
        this.imageService = imageService;
    }

    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {
        Podcast podcast = new Podcast();
        podcast.setType("RSS");
        podcast.setUrl(url);
        
        Document podcastXML;
        Element channel; 
        // Get information about podcast
        try {
            podcastXML = jdomService.parse(podcast.getUrl());
            channel = podcastXML.getRootElement().getChild("channel");
        } catch (JDOMException | IOException e) {
            logger.error("Error during parsing of podcast", e);
            throw new FindPodcastNotFoundException();
        }

        if (channel == null) {
            logger.error("Podcast has no channel");
            throw new FindPodcastNotFoundException();
        }
        
        
        podcast
                .setTitle(channel.getChildText("title"))
                .setDescription(channel.getChildText("description"))
                .setCover(imageService.getCoverFromURL(getPodcastCover(channel)));


        return podcast;
    }

    private String getPodcastCover(Element channelElement) {
        String cover;
        Element rssImage = channelElement.getChild("image");
        if (rssImage == null) {
            cover = null;
        } else {
            cover = rssImage.getChildText("url");
        }

        if (cover != null)
            return cover;

        Element itunesImage = channelElement.getChild("image", JdomService.ITUNES_NAMESPACE);
        if (itunesImage == null)
            return null;

        return itunesImage.getAttributeValue("href");
    }

}
