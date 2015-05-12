package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.xml.JdomService;
import lan.dk.podcastserver.utils.ImageUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 22/02/2014.
 */
@Component("JeuxVideoFRUpdater")
public class JeuxVideoFRUpdater extends AbstractUpdater {

    public static final String JEUXVIDEOFR_PATTERN = "dd/MM/yyyy";
    public static final String JEUXVIDEOFR_HOST_URL = "http://www.jeuxvideo.fr/";
    public static final String XML_PREFIX_DESCRIPTOR_URL = "http://www.jeuxvideo.fr/api/tv/xml.php?player_generique=player_generique&id=";
    public static final Pattern ID_JEUXVIDEOFR_PATTERN = Pattern.compile(".*-([0-9]*)\\..*");
    public static final String IMG_LOCALISATION_THUMB = "http://img.jeuxvideo.fr/03E80232%s";
    public static final String IMG_DELIMITER = "006E0046";

    @Resource JdomService jdomService;

    public Set<Item> getItems(Podcast podcast) {
        Document page;
        Set<Item> itemSet = new HashSet<>();

        try {
            Connection.Response response = Jsoup.connect(podcast.getUrl())
                    .timeout(10000)
                    .userAgent(USER_AGENT)
                    .referrer("http://www.google.fr")
                    .execute();
            page = response.parse();


            for (Element element : page.select(".block-video-tableVideo tbody tr")) {
                Item item = new Item()
                        .setTitle(element.select(".video a").attr("title"))
                        .setDescription(element.select(".video .bleu2").text())
                        .setCover(getCover(element.select(".tip img").attr("src")))
                        .setPubdate(fromJeuxVideoFr(element.select("td:nth-of-type(3)").text()));

                String itemUrl = element.select("a").attr("href");
                Matcher m = ID_JEUXVIDEOFR_PATTERN.matcher(itemUrl);
                if (m.find() && !m.group(1).equals("0") ) {
                    item.setUrl(itemUrl);
                    itemSet.add(item);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }

        return itemSet;
    }

    private Item getDetailFromXML(Item item, Integer idJeuxVideoFr) {

        logger.debug("Id JeuxVideoFr : {}", idJeuxVideoFr);

        org.jdom2.Document xmlEpisode = null;
        try {
            xmlEpisode = jdomService.jdom2Parse(XML_PREFIX_DESCRIPTOR_URL.concat(String.valueOf(idJeuxVideoFr)));
        } catch (JDOMException | IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return item;
        }

        org.jdom2.Element xml_item = xmlEpisode.getRootElement().getChild("channel").getChild("item");

        try {
            item.setCover(ImageUtils.getCoverFromURL(new URL(xml_item.getChildText("visuel_clip"))));
        } catch (IOException e) {
            e.printStackTrace();
        }


        return item;
    }

    @Override
    public String signatureOf(Podcast podcast) {
        Document page = null;

        try {
            page = Jsoup.connect(podcast.getUrl()).timeout(10000).get();
            return signatureService.generateMD5Signature(page.select(".block-video-tableVideo tbody tr").html());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }

        return "";
    }

    public Predicate<Item> notIn(Podcast podcast) {
        return item -> {
            String nameOfItem = FilenameUtils.getName(item.getUrl());

            return !podcast.getItems()
                    .stream()
                    .map(Item::getUrl)
                    .map(FilenameUtils::getName)
                    .anyMatch(nameOfItem::equals);
        };
    }

    public ZonedDateTime fromJeuxVideoFr(String pubDate) {
        return ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(pubDate, DateTimeFormatter.ofPattern(JEUXVIDEOFR_PATTERN)), LocalTime.of(0, 0)), ZoneId.of("Europe/Paris"));
    }

    private Cover getCover(String tinyUrl) throws IOException {
        if (StringUtils.isEmpty(tinyUrl)) {
            return null;
        }

        if (tinyUrl.contains(IMG_DELIMITER)) {
            return ImageUtils.getCoverFromURL(String.format(IMG_LOCALISATION_THUMB, tinyUrl.substring(tinyUrl.lastIndexOf(IMG_DELIMITER)+IMG_DELIMITER.length())));
        }

        /* http://2.im6.fr/-99663-photo-crop-pd41f08d993a0ab2c15f76ef444ac1e04-youtube.jpg?options=eNoryywqKU3N0bW0NDMzBgAi7gRK&width=400&height=300 */
        if (tinyUrl.contains("width") && tinyUrl.contains("height")) {
            return ImageUtils.getCoverFromURL(StringUtils.substringBeforeLast(tinyUrl, "&") + String.format("&width=400&height=300"));
        }
        
        return ImageUtils.getCoverFromURL(tinyUrl);
    }

    @Override
    public Type type() {
        return new Type("jeuxvideofr", "JeuxVideo.fr");
    }
}
