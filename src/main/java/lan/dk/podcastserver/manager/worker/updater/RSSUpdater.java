package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.xml.JdomService;
import lan.dk.podcastserver.utils.ImageUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Component("RSSUpdater")
public class RSSUpdater extends AbstractUpdater {

    @Resource JdomService jdomService;

    public Set<Item> getItems(Podcast podcast) {
        Set<Item> itemSet = new HashSet<>();
        // Si l'image de présentation a changé :
        Document podcastXMLSource;
        try {
            podcastXMLSource = jdomService.jdom2Parse(podcast.getUrl());
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }

        String currentCoverURL =
                (podcastXMLSource.getRootElement().getChild("channel").getChild("image") != null)
                        ? podcastXMLSource.getRootElement().getChild("channel").getChild("image").getChildText("url")
                        : null;

        Namespace media = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
        Namespace feedburner = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0");

        try {
            if (podcast.getCover() == null) {
                logger.debug("Traitement de la cover général du podcast");
                podcast.setCover(ImageUtils.getCoverFromURL(new URL(currentCoverURL)));
            }

            logger.debug("Traitement des Items");
            // Parcours des éléments :
            for (Element item : podcastXMLSource.getRootElement().getChild("channel").getChildren("item")) {
                if (item.getChild("enclosure") != null || item.getChild("origEnclosureLink", feedburner) != null)   { // est un podcast utilisable
                    Item podcastItem = new Item()
                            .setTitle(item.getChildText("title"))
                            .setPubdate(getPubDate(item))
                            .setDescription(item.getChildText("description"))
                            .setMimeType(item.getChild("enclosure").getAttributeValue("type"))
                            .setLength((StringUtils.isNotEmpty(item.getChild("enclosure").getAttributeValue("length")))
                                    ? Long.parseLong(item.getChild("enclosure").getAttributeValue("length"))
                                    : 0L);

                    if ((item.getChild("thumbnail", media) != null)) {
                        if (item.getChild("thumbnail", media).getAttributeValue("url") != null) {
                            podcastItem.setCover(ImageUtils.getCoverFromURL(new URL(item.getChild("thumbnail", media).getAttributeValue("url"))));
                        } else {
                            podcastItem.setCover(ImageUtils.getCoverFromURL(new URL(item.getChild("thumbnail", media).getText())));
                        }
                    }
                    // Gestion des cas pour l'url :
                    if (item.getChild("origEnclosureLink", feedburner) != null) {
                        podcastItem.setUrl(item.getChildText("origEnclosureLink", feedburner));
                    } else if (item.getChild("enclosure") != null) {
                        podcastItem.setUrl(item.getChild("enclosure").getAttributeValue("url"));
                    }

                    itemSet.add(podcastItem);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return itemSet;
    }

    private ZonedDateTime getPubDate(Element item) {
        try {
            return ZonedDateTime.parse(item.getChildText("pubDate"), DateTimeFormatter.RFC_1123_DATE_TIME);
        } catch (Exception e) {
            logger.error("Problem during date parsing", e);
        }
        // No better idea than returning null for unparseable date
        return null;
    }

    @Override
    public String generateSignature(Podcast podcast) {
        return signatureService.generateSignatureFromURL(podcast.getUrl());
    }


}
