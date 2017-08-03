package lan.dk.podcastserver.service;

import io.vavr.control.Option;
import io.vavr.control.Try;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.WatchList;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.Text;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

import static java.lang.Boolean.*;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Slf4j
@Service
@RequiredArgsConstructor
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
    private static final String URL_STRING = "url";
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
    private static final Namespace MEDIA_NAMESPACE = Namespace.getNamespace("media", "http://search.yahoo.com/mrss/");

    // URL Format
    private static final String LINK_PODCAST_FORMAT = "%s/api/podcasts/%s/rss";
    private static final String LINK_WATCHLIST_FORMAT = "%s/api/watchlists/%s/rss";
    private static final Comparator<Item> PUB_DATE_COMPARATOR = (one, another) -> one.getPubDate().isAfter(another.getPubDate()) ? -1 : 1;

    private final PodcastServerParameters podcastServerParameters;
    private final MimeTypeService mimeTypeService;
    private final UrlService urlService;

    public Option<Document> parse(String url) {
        return Try.of(() -> new SAXBuilder().build(urlService.asStream(url)))
                .onFailure(e -> log.error("Error during parsing of {}", url, e))
                .toOption();
    }

    public String podcastToXMLGeneric(Podcast podcast, String domainName, Boolean limit) throws IOException {
        return podcastToXMLGeneric( podcast, domainName, withNumberOfItem(podcast, limit));
    }

    private String podcastToXMLGeneric(Podcast podcast, String domainName, Long limit) throws IOException {

        Long limitOfItem = (limit == null) ? podcast.getItems().size() : limit;

        String coverUrl = getCoverUrl(podcast.getCover(), domainName);

        Element channel = new Element(CHANNEL)
            .addContent(new Element(TITLE).addContent(new Text(podcast.getTitle())))
            .addContent(new Element(LINK).addContent(new Text(String.format(LINK_PODCAST_FORMAT, domainName, podcast.getId()))))
            .addContent(new Element(DESCRIPTION).addContent(new Text(podcast.getDescription())))
            .addContent(new Element(SUBTITLE, ITUNES_NAMESPACE).addContent(new Text(podcast.getDescription())))
            .addContent(new Element(SUMMARY, ITUNES_NAMESPACE).addContent(new Text(podcast.getDescription())))
            .addContent(new Element(LANGUAGE).addContent(new Text("fr-fr")))
            .addContent(new Element(AUTHOR, ITUNES_NAMESPACE).addContent(new Text(podcast.getType())))
            .addContent(new Element(CATEGORY, ITUNES_NAMESPACE));

        if (nonNull(podcast.getLastUpdate())) {
            channel
                .addContent(new Element(PUB_DATE)
                        .addContent(new Text(podcast.getLastUpdate().format(DateTimeFormatter.RFC_1123_DATE_TIME)))
                );
        }

        if (podcast.getCover() != null) {
            Element itunesImage = new Element(IMAGE, ITUNES_NAMESPACE);
            Element image = new Element(IMAGE);

            itunesImage.addContent(new Text(coverUrl));

            image
                    .addContent(new Element(HEIGHT).addContent(String.valueOf(podcast.getCover().getHeight())))
                    .addContent(new Element(URL_STRING).addContent(coverUrl))
                    .addContent(new Element(WIDTH).addContent(String.valueOf(podcast.getCover().getWidth())));

            channel
                    .addContent(image)
                    .addContent(itunesImage);
        }


        podcast.getItems()
                .stream()
                .filter(i -> nonNull(i.getPubDate()))
                .sorted(PUB_DATE_COMPARATOR)
                .limit(limitOfItem)
                .map(this.itemToElementWithDomain(domainName))
                .forEachOrdered(channel::addContent);

        return channelToRss(channel);
    }

    public String watchListToXml(WatchList watchList, String domainName) {
        Element channel = new Element(CHANNEL)
            .addContent(new Element(TITLE).addContent(new Text(watchList.getName())))
            .addContent(new Element(LINK).addContent(new Text(String.format(LINK_WATCHLIST_FORMAT, domainName, watchList.getId()))));

        watchList.getItems().stream()
                .filter(i -> nonNull(i.getPubDate()))
                .sorted(PUB_DATE_COMPARATOR)
                .map(this.itemToElementWithDomain(domainName))
                .forEachOrdered(channel::addContent);

        return channelToRss(channel);
    }

    private Function<Item, Element> itemToElementWithDomain(String domainName) {
        return item -> {
            /* Cover */
            String itemCoverUrl = getCoverUrl(item.getCoverOfItemOrPodcast(), domainName);
            Element itunesItemThumbnail = new Element(IMAGE, ITUNES_NAMESPACE).setContent(new Text(itemCoverUrl));
            Element thumbnail = new Element(THUMBNAIL, MEDIA_NAMESPACE).setAttribute(URL_STRING, itemCoverUrl);

            /* Enclosure */
            Element item_enclosure = new Element(ENCLOSURE).setAttribute(URL_STRING, domainName
                    .concat(item.getProxyURLWithoutExtention())
                    .concat((item.isDownloaded()) ? "." + FilenameUtils.getExtension(item.getFileName()) : mimeTypeService.getExtension(item)));
            if (item.getLength() != null) item_enclosure.setAttribute(LENGTH, String.valueOf(item.getLength()));
            if (!isEmpty(item.getMimeType())) item_enclosure.setAttribute(TYPE, item.getMimeType());

            return new Element(ITEM)
                    .addContent(new Element(TITLE).addContent(new Text(item.getTitle())))
                    .addContent(new Element(DESCRIPTION).addContent(new Text(item.getDescription())))
                    .addContent(new Element(PUB_DATE).addContent(new Text(item.getPubDate().format(DateTimeFormatter.RFC_1123_DATE_TIME))))
                    .addContent(new Element(EXPLICIT, ITUNES_NAMESPACE).addContent(new Text(NO)))
                    .addContent(new Element(SUBTITLE, ITUNES_NAMESPACE).addContent(new Text(item.getTitle())))
                    .addContent(new Element(SUMMARY, ITUNES_NAMESPACE).addContent(new Text(item.getDescription())))
                    .addContent(new Element(GUID).addContent(new Text(domainName + item.getProxyURL())))
                    .addContent(itunesItemThumbnail)
                    .addContent(thumbnail)
                    .addContent(item_enclosure);
        };
    }

    private String channelToRss(Element channel) {
        Element rss = new Element(RSS).addContent(channel);
        rss.addNamespaceDeclaration(ITUNES_NAMESPACE);
        rss.addNamespaceDeclaration(MEDIA_NAMESPACE);

        return Try.of(StringWriter::new)
            .andThenTry(sw -> new XMLOutputter(Format.getPrettyFormat()).output(new Document(rss), sw))
            .map(StringWriter::toString)
            .getOrElseThrow(e -> new RuntimeException("Error during generation of RSS", e));
    }

    private long withNumberOfItem(Podcast podcast, Boolean limit) {
        return Objects.equals(TRUE, limit) ? podcastServerParameters.getRssDefaultNumberItem() : podcast.getItems().size();
    }

    private String getCoverUrl(Cover cover, String domainName) {
        return cover.getUrl().startsWith("/") ? (domainName + cover.getUrl()) : cover.getUrl();
    }
}
