package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.DateUtils;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 22/02/2014.
 */
@Component("JeuxVideoFRUpdater")
@Scope("prototype")
public class JeuxVideoFRUpdater extends AbstractUpdater {

    public static String JEUXVIDEOFR_HOST_URL = "http://www.jeuxvideo.fr/";
    public static String XML_PREFIX_DESCRIPTOR_URL = "http://www.jeuxvideo.fr/api/tv/xml.php?player_generique=player_generique&id=";
    public static Pattern ID_JEUXVIDEOFR_PATTERN = Pattern.compile(".*-([0-9]*)\\..*");

    @Override
    public Podcast updateFeed(Podcast podcast) {

        Document page = null;

        try {
            Connection.Response response = Jsoup.connect(podcast.getUrl())
                    .userAgent(USER_AGENT)
                    .referrer("http://www.google.fr")
                    .execute();
            page = response.parse();


            for (Element element : page.select("#playlist ul li")) {
                Item item = new Item()
                                .setTitle(element.select("a").first().attr("title"))
                                .setDescription(element.select("a").first().attr("title"));
                try {
                    item.setPubdate(DateUtils.jeuxVideoFrToTimeStamp(element.select("a span").text()));
                } catch (ParseException e) {
                    logger.error("Non Parseable : {}", element.select("a span").text(), e);
                }

                Matcher m = ID_JEUXVIDEOFR_PATTERN.matcher(element.select("a").attr("href"));
                if (m.find()) {
                    item = getDetailFromXML(item, Integer.valueOf(m.group(1)));
                } else {
                    break;
                }

                if (!podcastContains(podcast, item)) {
                    // Si le bean est valide :
                    item.setPodcast(podcast);
                    Set<ConstraintViolation<Item>> constraintViolations = validator.validate( item );
                    if (constraintViolations.isEmpty()) {
                        podcast.getItems().add(item);
                    } else {
                        logger.error(constraintViolations.toString());
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }


        return podcast;
    }

    private Item getDetailFromXML(Item item, Integer idJeuxVideoFr) {

        logger.debug("Id JeuxVideoFr : {}", idJeuxVideoFr);

        org.jdom2.Document xmlEpisode = null;
        try {
            xmlEpisode = jDomUtils.jdom2Parse(XML_PREFIX_DESCRIPTOR_URL.concat(String.valueOf(idJeuxVideoFr)));
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("JDOMException :", e);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }

        org.jdom2.Element xml_item = xmlEpisode.getRootElement().getChild("channel").getChild("item");

        try {
            item.setCover(ImageUtils.getCoverFromURL(new URL(xml_item.getChildText("visuel_clip"))));

            if (StringUtils.isNotEmpty(xml_item.getChildText("url_video_hq"))) {
                item.setUrl(xml_item.getChildText("url_video_hq"));
            } else if (StringUtils.isNotEmpty(xml_item.getChildText("url_video_sd"))) {
                item.setUrl(xml_item.getChildText("url_video_sd"));
            } else if (StringUtils.isNotEmpty(xml_item.getChildText("url_video_3g"))) {
                item.setUrl(xml_item.getChildText("url_video_3g"));
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
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
    public String signaturePodcast(Podcast podcast) {
        Document page = null;

        try {
            page = Jsoup.connect(podcast.getUrl()).get();
            return DigestUtils.generateMD5SignatureFromDOM(page.select("#playlist ul li").html());
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
}
