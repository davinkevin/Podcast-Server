package lan.dk.podcastserver.manager.worker.finder;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.FindPodcastNotFoundException;
import lan.dk.podcastserver.service.HtmlService;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Created by kevin on 22/02/15.
 */
@Service("YoutubeFinder")
public class YoutubeFinder implements Finder {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private HtmlService htmlService;

    @Autowired
    public YoutubeFinder(HtmlService htmlService) {
        this.htmlService = htmlService;
    }

    @Override
    public Podcast find(String url) throws FindPodcastNotFoundException {

        Document page;

        try {
            page = htmlService.connectWithDefault(url).get();
        } catch (IOException e) {
            logger.error("IOException :", e);
            throw new FindPodcastNotFoundException();
        }

        Podcast youtubePodcst = new Podcast()
                .setUrl(url)
                .setType("Youtube")
                .setTitle(getTitle(page))
                .setDescription(getDescription(page));

        youtubePodcst
                .setCover(getCover(page));

        return youtubePodcst;
    }

    private String getDescription(Document page) {
        Element elementWithExternalId = page.select("meta[name=description]").first();
        if (elementWithExternalId != null) {
            return elementWithExternalId.attr("content");
        }

        return "";
    }

    private String getTitle(Document page) {
        Element elementWithExternalId = page.select("meta[name=title]").first();
        if (elementWithExternalId != null) {
            return elementWithExternalId.attr("content");
        }

        return "";
    }

    private Cover getCover(Document page) {
        Element elementWithExternalId = page.select("img.channel-header-profile-image").first();
        if (elementWithExternalId != null) {
            return new Cover(elementWithExternalId.attr("src"), 200, 200);
        }

        return new Cover();
    }
}
