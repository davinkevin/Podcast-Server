package lan.dk.podcastserver.manager.worker.updater;

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

//@Scope("prototype")
@Component("RSSUpdater")
@Scope("prototype")
public class RSSUpdater extends AbstractUpdater {

    public Podcast updateFeed(Podcast podcast) {

        // Si l'image de présentation a changé :
        Document podcastXMLSource = null;
        try {
            podcastXMLSource = jDomUtils.jdom2Parse(podcast.getUrl());
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
            logger.debug("Traitement de la cover général du podcast");
            if (podcast.getCover() != null && currentCoverURL != null && !currentCoverURL.equals(podcast.getCover().getURL())) {
                podcast.setCover(ImageUtils.getCoverFromURL(new URL(currentCoverURL)));
            }
            logger.debug("Traitement des Items");
            // Parcours des éléments :
            for (Element item : podcastXMLSource.getRootElement().getChild("channel").getChildren("item")) {

                Item podcastItem = new Item()
                                            .setTitle(item.getChildText("title"))
                                            .setPubdate(DateUtils.rfc2822DateToTimeStamp(item.getChildText("pubDate")))
                                            .setDescription(item.getChildText("description"))
                                            .setCover((item.getChild("thumbnail", media) != null) ? ImageUtils.getCoverFromURL(new URL(item.getChild("thumbnail", media).getAttributeValue("url"))) : null)
                                            .setMimeType(item.getChild("enclosure").getAttributeValue("type"))
                                            .setLength((item.getChild("enclosure").getAttributeValue("length") != null)
                                                    ? Long.parseLong(item.getChild("enclosure").getAttributeValue("length"))
                                                    : 0L);
                 // Gestion des cas pour l'url :
                if (item.getChild("origEnclosureLink", feedburner) != null) {
                    podcastItem.setUrl(item.getChildText("origEnclosureLink", feedburner));
                } else if (item.getChild("enclosure") != null) {
                    podcastItem.setUrl(item.getChild("enclosure").getAttributeValue("url"));
                }

                // Sauvegarde
                if ( !podcast.getItems().contains(podcastItem)) {
                    podcast.getItems().add(podcastItem);
                    podcastItem.setPodcast(podcast);
                    if (podcastItem.getCover() == null) {
                        podcastItem.setCover(podcast.getCover());
                    }
                    logger.debug("Ajout du nouvel episode : " + podcastItem.toString());
                }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return podcast;
    }

    @Override
    public Podcast findPodcast(String url) {
        Podcast podcast = new Podcast();
        podcast.setUrl(url);
        Document podcastXML = null;
        try {
            podcastXML = jDomUtils.jdom2Parse(podcast.getUrl());
            if (podcastXML.getRootElement().getChild("channel").getChildText("title") != null) {
                podcast.setTitle(podcastXML.getRootElement().getChild("channel").getChildText("title"));
            }
            if (podcastXML.getRootElement().getChild("channel").getChild("image").getChildText("url") != null) {
                podcast.setCover(ImageUtils.getCoverFromURL(new URL(podcastXML.getRootElement().getChild("channel").getChild("image").getChildText("url"))));
            }
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return podcast;
    }

    @Override
    public String signaturePodcast(Podcast podcast) {
        return DigestUtils.generateMD5SignatureFromUrl(podcast.getUrl());
    }


}
