package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.DigestUtils;
import lan.dk.podcastserver.utils.ImageUtils;
import lan.dk.podcastserver.utils.jDomUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 22/02/2014.
 */
@Component("JeuxVideoFRUpdater")
@Scope("prototype")
public class JeuxVideoFRUpdater extends AbstractUpdater {

    public static final String JEUXVIDEOFR_PATTERN = "dd/MM/yyyy";
    public static final String JEUXVIDEOFR_HOST_URL = "http://www.jeuxvideo.fr/";
    public static final String XML_PREFIX_DESCRIPTOR_URL = "http://www.jeuxvideo.fr/api/tv/xml.php?player_generique=player_generique&id=";
    public static final Pattern ID_JEUXVIDEOFR_PATTERN = Pattern.compile(".*-([0-9]*)\\..*");
    public static final String IMG_LOCALISATION_THUMB = "http://img.jeuxvideo.fr/03E80232%s";
    public static final String IMG_DELIMITER = "006E0046";

    @Override
    public Podcast updateAndAddItems(Podcast podcast) {

        Set<Item> itemSet = getItems(podcast);

        // Si le bean est valide :
        itemSet.stream()
                .filter(item -> !podcastContains(podcast, item))
                .forEach(item -> {
                    // Si le bean est valide :
                    item.setPodcast(podcast);
                    Set<ConstraintViolation<Item>> constraintViolations = validator.validate(item);
                    if (constraintViolations.isEmpty()) {
                        podcast.getItems().add(item);
                    } else {
                        logger.error(constraintViolations.toString());
                    }
                });

        return podcast;
    }

    public Set<Item> getItems(Podcast podcast) {
        Document page;
        Set<Item> itemSet = new HashSet<>();

        try {
            Connection.Response response = Jsoup.connect(podcast.getUrl())
                    .timeout(5000)
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
            xmlEpisode = jDomUtils.jdom2Parse(XML_PREFIX_DESCRIPTOR_URL.concat(String.valueOf(idJeuxVideoFr)));
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
    public Podcast findPodcast(String url) {
        return null;
    }

    @Override
    public String generateSignature(Podcast podcast) {
        Document page = null;

        try {
            page = Jsoup.connect(podcast.getUrl()).timeout(5000).get();
            return DigestUtils.generateMD5SignatureFromDOM(page.select(".block-video-tableVideo tbody tr").html());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }

        return "";
    }

    private boolean podcastContains(Podcast podcast, Item item) {
        String nameOfItem = FilenameUtils.getName(item.getUrl());
        for (Item itemInPodcast : podcast.getItems()) {
            if (nameOfItem.equals(FilenameUtils.getName(itemInPodcast.getUrl()))) {
                return true;
            }
        }
        return false;
    }

    public ZonedDateTime fromJeuxVideoFr(String pubDate) {
        return ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(pubDate, DateTimeFormatter.ofPattern(JEUXVIDEOFR_PATTERN)), LocalTime.of(0, 0)), ZoneId.of("Europe/Paris"));
    }

    private Cover getCover(String tinyUrl) throws IOException {
        if (StringUtils.isEmpty(tinyUrl) || !tinyUrl.contains(IMG_DELIMITER)) {
            return null;
        }

        String coverUrl = String.format(IMG_LOCALISATION_THUMB, tinyUrl.substring(tinyUrl.lastIndexOf(IMG_DELIMITER)+IMG_DELIMITER.length()));

        return ImageUtils.getCoverFromURL(new URL(coverUrl));

    }
}
