package lan.dk.podcastserver.service;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.*;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;

import static java.util.Objects.nonNull;

@Slf4j
@Service
public class JdomService {

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
    public static final Namespace ITUNES_NAMESPACE = Namespace.getNamespace("itunes", "http://www.itunes.com/dtds/podcast-1.0.dtd");
    public static final Namespace MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");

    // URL Format
    private static final String LINK_FORMAT = "%s/api/podcast/%d/rss";
    public static final Comparator<Item> PUBDATE_COMPARATOR = (one, another) -> one.getPubdate().isAfter(another.getPubdate()) ? -1 : 1;

    final PodcastServerParameters podcastServerParameters;
    final MimeTypeService mimeTypeService;
    final UrlService urlService;

    @Autowired
    public JdomService(PodcastServerParameters podcastServerParameters, MimeTypeService mimeTypeService, UrlService urlService) {
        this.podcastServerParameters = podcastServerParameters;
        this.mimeTypeService = mimeTypeService;
        this.urlService = urlService;
    }

    public Document parse(String urlasString) throws JDOMException, IOException {
        Document doc;
        try {
            log.debug("Begin Parsing of {}", urlasString);
            doc = new SAXBuilder().build(urlService.getConnection(urlasString).getInputStream(), urlasString);
            log.debug("End Parsing of {}", urlasString);
        } catch (JDOMException | IOException e) {
            log.error("Error during parsing of {}", urlasString, e);
            throw e;
        }

        return doc;
    }


    public String podcastToXMLGeneric (Podcast podcast, Boolean limit) throws IOException {
        return podcastToXMLGeneric( podcast, withNumberOfItem(podcast, limit));
    }

    private long withNumberOfItem(Podcast podcast, Boolean limit) {
        return Boolean.TRUE.equals(limit) ? podcastServerParameters.rssDefaultNumberItem() : podcast.getItems().size();
    }

    public String podcastToXMLGeneric (Podcast podcast, Long limit) throws IOException {

        Long limitOfItem = (limit == null) ? podcast.getItems().size() : limit;

        Element channel = new Element(CHANNEL);

        String coverUrl = StringUtils.replace(podcast.getCover().getUrl(), " ", "%20");

        Element title = new Element(TITLE);
        title.addContent(new Text(podcast.getTitle()));

        Element url = new Element(LINK);
        url.addContent(new Text(String.format(LINK_FORMAT, podcastServerParameters.getServerUrl(), podcast.getId())));

        if (nonNull(podcast.getLastUpdate())) {
            Element lastUpdate = new Element(PUB_DATE);
            lastUpdate.addContent(new Text(podcast.getLastUpdate().format(DateTimeFormatter.RFC_1123_DATE_TIME)));
            channel.addContent(lastUpdate);
        }

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
        /*channel.addContent(lastUpdate);*/
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
        .filter(i -> Objects.nonNull(i.getPubdate()))
        .sorted(PUBDATE_COMPARATOR)
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

            item_enclosure.setAttribute(URL, podcastServerParameters.getServerUrl()
                    .concat(item.getProxyURLWithoutExtention())
                    .concat((item.isDownloaded()) ? "." + FilenameUtils.getExtension(item.getFileName()) : mimeTypeService.getExtension(item)));

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
            guid.addContent(new Text(podcastServerParameters.getServerUrl() + item.getProxyURL()));
            xmlItem.addContent(guid);

            Element itunesItemThumbnail = new Element(IMAGE, ITUNES_NAMESPACE);
            itunesItemThumbnail.setContent(new Text(StringUtils.replace(item.getCoverOfItemOrPodcast().getUrl(), " ", "%20")));
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

        Writer writer = new StringWriter();

        new XMLOutputter(Format.getPrettyFormat()).output(new Document(rss), writer);
        return writer.toString();
    }
}
