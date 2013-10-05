package lan.dk.podcastserver.utils;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;


public class jDomUtils {

    private static Logger logger = LoggerFactory.getLogger(jDomUtils.class);

    public static Podcast getPodcastFromURL(URL url) {
        Podcast podcast = new Podcast();
        podcast.setUrl(url.toString());
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

    public static Document jdom2Parse(String urlasString) throws JDOMException, IOException {
        SAXBuilder sax = new SAXBuilder();
        URL url;
        Document doc = null;
        logger.debug("Debut Parsing");
        try {
            url = new URL(urlasString);
            doc = sax.build(url);
            logger.debug("Fin Parsing");
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }

        return doc;

    }

    public static String podcastToXMLGeneric (Podcast podcast, String serveurURL) {
        Element channel = new Element("channel");

        Element title = new Element("title");
        title.addContent(new Text(podcast.getTitle()));

        Element url = new Element("link");
        url.addContent(new Text(podcast.getUrl()));

        Element lastUpdate = new Element("pubDate");
        lastUpdate.addContent(new Text(DateUtils.TimeStampToRFC2822(podcast.getLastUpdate())));

        Element description = new Element("description");
        description.addContent(new Text(podcast.getDescription()));

        channel.addContent(url);
        channel.addContent(title);
        channel.addContent(lastUpdate);
        channel.addContent(description);

        Element image = new Element("image");
        Element image_url = new Element("url");
        Element image_width = new Element("width");
        Element image_height = new Element("height");
        image_url.addContent(podcast.getCover().getURL());
        image_width.addContent(String.valueOf(podcast.getCover().getWidth()));
        image_height.addContent(String.valueOf(podcast.getCover().getHeight()));
        image.addContent(image_height);
        image.addContent(image_url);
        image.addContent(image_width);
        channel.addContent(image);

        for (Item item : podcast.getItems()) {
            Element xml_item = new Element("item");

            Element item_title = new Element("title");
            item_title.addContent(new Text(item.getTitle()));
            xml_item.addContent(item_title);

            Element item_description = new Element("description");
            item_description.addContent(new Text(item.getDescription()));
            xml_item.addContent(item_description);

            Element item_enclosure = new Element("enclosure");
            item_enclosure.setAttribute("url", serveurURL + item.getProxyURL());
            item_enclosure.setAttribute("length", "0");
            item_enclosure.setAttribute("type", "video/mp4");
            xml_item.addContent(item_enclosure);

            Element item_pubdate = new Element("pubDate");
            item_pubdate.addContent(new Text(DateUtils.TimeStampToRFC2822(item.getPubdate())));
            xml_item.addContent(item_pubdate);

             channel.addContent(xml_item);
        }

        Element rss = new Element("rss");
        rss.addNamespaceDeclaration(Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd"));
        rss.addContent(channel);

        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());
        Writer writer = new StringWriter();
        try {
            xout.output(new Document(rss), writer);
            return writer.toString();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return null;

    }
}
