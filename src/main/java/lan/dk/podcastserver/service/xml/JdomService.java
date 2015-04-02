package lan.dk.podcastserver.service.xml;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.utils.MimeTypeUtils;
import lan.dk.podcastserver.utils.URLUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;

@Service
public class JdomService {

    private static final Logger logger = LoggerFactory.getLogger(JdomService.class);
    
    // Element names : 
    private static final String CHANNEL = "channel";
    private static final String TITLE = "title";
    private static final String LINK = "link";
    private static final String PUB_DATE = "pubDate";
    private static final String DESCRIPTION = "description";
    private static final String SUBTITLE = "subtitle";
    private static final String SUMMARY = "summary";
    private static final String LANGUAGE = "language";
    private static final String AUTHOR = "author";
    private static final String CATEGORY = "category";
    private static final String IMAGE = "image";
    private static final String URL = "url";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String ITEM = "item";
    private static final String ENCLOSURE = "enclosure";
    private static final String LENGTH = "length";
    private static final String TYPE = "type";
    private static final String EXPLICIT = "explicit";
    private static final String NO = "No";
    private static final String GUID = "guid";
    private static final String THUMBNAIL = "thumbnail";
    private static final String RSS = "rss";

    //Useful namespace : 
    private static final Namespace ITUNES_NAMESPACE = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd");
    private static final Namespace MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");
    
    // Sax Parser not used in final stage, because it is not thread-safe...:
    /*private static final SAXBuilder SAX_BUILDER = new SAXBuilder();*/
    
    // URL Format
    private static final String LINK_FORMAT = "%s/api/podcast/%d/rss";
    
    @Resource PodcastServerParameters podcastServerParameters;

    public Document jdom2Parse(String urlasString) throws JDOMException, IOException {
        Document doc;
        try {
            logger.debug("Begin Parsing of {}", urlasString);
            doc = new SAXBuilder().build(URLUtils.getStreamWithTimeOut(urlasString).getInputStream(), urlasString);
            logger.debug("End Parsing of {}", urlasString);
        } catch (JDOMException | IOException e) {
            logger.error("Error during parsin of {}", urlasString, e);
            throw e;
        }

        return doc;
    }

    public String podcastToXMLGeneric (Podcast podcast) {
        return podcastToXMLGeneric(podcast, podcastServerParameters.rssDefaultNumberItem());
    }

    public String podcastToXMLGeneric (Podcast podcast, Long limit) {

        Long limitOfItem = (limit == null) ? podcast.getItems().size() : limit;

        Element channel = new Element(CHANNEL);

        String coverUrl = podcast.getCover().getUrl();
        
        Element title = new Element(TITLE);
        title.addContent(new Text(podcast.getTitle()));

        Element url = new Element(LINK);
        url.addContent(new Text(String.format(LINK_FORMAT, podcastServerParameters.getServeurURL(), podcast.getId())));

        Element lastUpdate = new Element(PUB_DATE);
        lastUpdate.addContent(new Text(podcast.getLastUpdate().format(DateTimeFormatter.RFC_1123_DATE_TIME)));

        Element description = new Element(DESCRIPTION);
        description.addContent(new Text(podcast.getDescription()));

        Element itunesSub = new Element(SUBTITLE, ITUNES_NAMESPACE);
        itunesSub.addContent(new Text(podcast.getDescription()));

        Element itunesSummary = new Element(SUMMARY, ITUNES_NAMESPACE);
        itunesSummary.addContent(new Text(podcast.getDescription()));

        Element language = new Element(LANGUAGE);
        language.addContent(new Text("fr-fr"));

        //Element copyright = new Element("copyright");
        //copyright.addContent(new Text(podcast.getTitle()));

        Element itunesAuthor = new Element(AUTHOR, ITUNES_NAMESPACE);
        itunesAuthor.addContent(new Text(podcast.getType()));

        Element itunesCategory = new Element(CATEGORY, ITUNES_NAMESPACE);


        channel.addContent(url);
        channel.addContent(title);
        channel.addContent(lastUpdate);
        channel.addContent(description);
        channel.addContent(itunesSub);
        channel.addContent(itunesSummary);
        channel.addContent(language);
        //channel.addContent(copyright);
        channel.addContent(itunesAuthor);
        channel.addContent(itunesCategory);


        Element itunesImage = new Element(IMAGE, ITUNES_NAMESPACE);
        if (podcast.getCover() != null) {
            Element image = new Element(IMAGE);
            Element image_url = new Element(URL);
            Element image_width = new Element(WIDTH);
            Element image_height = new Element(HEIGHT);

            itunesImage.addContent(new Text(coverUrl));

            image_url.addContent(coverUrl);
            image_width.addContent(String.valueOf(podcast.getCover().getWidth()));
            image_height.addContent(String.valueOf(podcast.getCover().getHeight()));
            image.addContent(image_height);
            image.addContent(image_url);
            image.addContent(image_width);
            channel.addContent(image);
            channel.addContent(itunesImage);
        }

       podcast.getItems()
        .stream()
        .limit(limitOfItem)
        .forEachOrdered(item -> {
            Element xmlItem = new Element(ITEM);

            Element item_title = new Element(TITLE);
            item_title.addContent(new Text(item.getTitle()));
            xmlItem.addContent(item_title);

            Element item_description = new Element(DESCRIPTION);
            item_description.addContent(new Text(item.getDescription()));
            xmlItem.addContent(item_description);

            Element item_enclosure = new Element(ENCLOSURE);

            item_enclosure.setAttribute(URL, podcastServerParameters.getServeurURL()
                    .concat(item.getProxyURLWithoutExtention())
                    .concat((item.isDownloaded()) ? "." + FilenameUtils.getExtension(item.getFileName()) : MimeTypeUtils.getExtension(item)));

            if (item.getLength() != null) {
                item_enclosure.setAttribute(LENGTH, String.valueOf(item.getLength()));
            }

            if (StringUtils.isNotEmpty(item.getMimeType()))
                item_enclosure.setAttribute(TYPE, item.getMimeType());

            xmlItem.addContent(item_enclosure);

            Element item_pubdate = new Element(PUB_DATE);
            item_pubdate.addContent(new Text(item.getPubdate().format(DateTimeFormatter.RFC_1123_DATE_TIME)));
            xmlItem.addContent(item_pubdate);

            Element itunesExplicite = new Element(EXPLICIT, ITUNES_NAMESPACE);
            itunesExplicite.addContent(new Text(NO));
            xmlItem.addContent(itunesExplicite);

            Element itunesItemSub = new Element(SUBTITLE, ITUNES_NAMESPACE);
            itunesItemSub.addContent(new Text(item.getTitle()));
            xmlItem.addContent(itunesItemSub);

            Element itunesItemSummary = new Element(SUMMARY, ITUNES_NAMESPACE);
            itunesItemSummary.addContent(new Text(item.getDescription()));
            xmlItem.addContent(itunesItemSummary);

            Element guid = new Element(GUID);
            guid.addContent(new Text(podcastServerParameters.getServeurURL() + item.getProxyURL()));
            xmlItem.addContent(guid);

            Element itunesItemThumbnail = new Element(IMAGE, ITUNES_NAMESPACE);
            itunesItemThumbnail.setContent(new Text(item.getCoverOfItemOrPodcast().getUrl()));
            xmlItem.addContent(itunesItemThumbnail);

            Element thumbnail = new Element(THUMBNAIL, MEDIA_NAMESPACE);
            thumbnail.setAttribute(URL, item.getCoverOfItemOrPodcast().getUrl());
            xmlItem.addContent(thumbnail);

            channel.addContent(xmlItem);
        });

        Element rss = new Element(RSS);
        rss.addNamespaceDeclaration(ITUNES_NAMESPACE);
        rss.addNamespaceDeclaration(MEDIA_NAMESPACE);
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
