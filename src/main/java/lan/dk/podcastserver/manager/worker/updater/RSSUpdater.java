package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.DateUtils;
import lan.dk.podcastserver.utils.ImageUtils;
import lan.dk.podcastserver.utils.jDomUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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

        String currentCoverURL = podcastXMLSource.getRootElement().getChild("channel").getChild("image").getChildText("url");
        Namespace media = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
        Namespace feedburner = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0");

        try {
            logger.debug("Traitement de la cover");
            if (podcast.getCover() != null && !currentCoverURL.equals(podcast.getCover().getURL())) {
                podcast.setCover(ImageUtils.getCoverFromURL(new URL(currentCoverURL)));
            }
            logger.debug("Traitement des Items");
            // Parcours des éléments :
            for (Element item : podcastXMLSource.getRootElement().getChild("channel").getChildren("item")) {

                Item podcastItem = new Item(item.getChildText("title"), item.getChild("enclosure").getAttributeValue("url"), DateUtils.rfc2822DateToTimeStamp(item.getChildText("pubDate")));

                if ( !podcast.getItems().contains(podcastItem)) {
                    podcast.getItems().add(podcastItem);
                    podcastItem.setPodcast(podcast);
                    if (podcastItem.getCover() == null) {
                        podcastItem.setCover(podcast.getCover());
                    }
                    logger.debug("Ajout du nouvel episode : " + podcastItem.toString());
                }

            }
            //podcast = podcastService.update(podcast, podcast.getId());

            boolean xpath = false;


            logger.debug("Traitement du flux en XPATH");

            XPathFactory xpfac = XPathFactory.instance();
            XPathExpression xp = null;
            Element coverURL;
            for (Item item : podcast.getItems()) {
                xp = xpfac.compile("//item[enclosure/@url=\"" + item.getUrl() +"\"]", Filters.element());
                for (Object elem : xp.evaluate(podcastXMLSource)) {
                    Element element = (Element) elem;
                    //logger.debug("Item Found in XML " + element.getChildText("title"));
                    element.getChild("enclosure").setAttribute("url", this.getServerURL() + "/api/item/" + item.getId() + "/download");

                    // Suppression des tags complémentaire :
                    element.removeChild("origLink", feedburner);
                    element.removeChild("origEnclosureLink", feedburner);


                    try {
                        //Si le podcast possède un Thumbnail :
                        coverURL = element.getChild("thumbnail", media);
                        if (coverURL != null && coverURL.getAttributeValue("url") != null && !coverURL.getAttributeValue("url").equals(item.getCover().getURL())) {
                            //logger.debug(coverURL.getAttributeValue("url"));
                            item.setCover(ImageUtils.getCoverFromURL(new URL(coverURL.getAttributeValue("url"))));
                        } else {
                            item.setCover(podcast.getCover());
                        }
                    } catch (IOException e) {
                        item.setCover(podcast.getCover());
                    }

                }
            }

            XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
            Writer writer = new StringWriter();
            xout.output(podcastXMLSource, writer);

            logger.debug("Sauvegarde du podcast");
            //logger.debug("RSS Feed : " + writer.toString());
            podcast.setRssFeed(writer.toString());

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
}
