package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.*;
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
import java.text.ParseException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 22/02/2014.
 *
 * Utilisation de referrer et User-Agent : http://stackoverflow.com/questions/6581655/jsoup-useragent-how-to-set-it-right
 *
 */
@Component("BeInSportsUpdater")
@Scope("prototype")
public class BeInSportUpdater extends AbstractUpdater {

    public static String EPISODE_LISTING_URL = "http://www.beinsports.fr/replay/category/{idBeInSport}/page/1/size/8/ajax/true";
    public static String XML_PREFIX_DESCRIPTOR_URL = "http://www.beinsports.fr/fragment/beINSport/xml/vodConfig/videoId/";
    public static String BEINSPORTS_HOST_URL = "http://www.beinsports.fr/";
    /* Prefixe for HTTP content over RTMP */
    public static String[] HTTP_VOD_PREFIX_URL = {
                                        "http://vod.beinsports1.aka.oss1.performgroup.com/",
                                        "http://vod.cms.download.performgroup.com/beinsport/"
    };

    /* Patter to extract value from URL */
    public static Pattern IDBEINSPORT_PATTERN = Pattern.compile(".*/category/([^/]*)/.*");
    public static Pattern SUBSTRING_VIDEO_URL_PATTERN = Pattern.compile(".*/([0-9]*/.*)$");

    @Override
    public Podcast updateFeed(Podcast podcast) {
        Document page = null;
        String listingUrl = getListingUrl(podcast);
        try {

            Connection.Response response = Jsoup.connect(listingUrl)
                    .timeout(5000)
                    .userAgent(USER_AGENT)
                    .referrer("http://www.google.fr")
                    .execute();
            page = response.parse();

            //logger.debug(page.html());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }

        if (page != null) {
            for(Element article : page.select("article")) {
                Item item = new Item()
                                    .setTitle(article.select(".info h4 a").first().text())
                                    .setDescription(article.select(".info span").first().text());

                item = getDetailOfItemByXML(item, article.select("a").first().className());

                logger.debug(item.toString());

                if (!podcast.getItems().contains(item)) {

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
        }

        return podcast;
    }

    private Item getDetailOfItemByXML(Item item, String idItemBeInSport) {
        logger.debug("Id BeInSport : {}", idItemBeInSport);

        org.jdom2.Document xmlEpisode = null;
        try {
            xmlEpisode = jDomUtils.jdom2Parse(XML_PREFIX_DESCRIPTOR_URL.concat(idItemBeInSport));
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("JDOMException :", e);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }

        org.jdom2.Element xml_clip = xmlEpisode.getRootElement().getChild("clip");

        try {
            item.setPubdate(DateUtils.beInSportDateToTimeStamp(xml_clip.getAttributeValue("videoCreationDate")));
            item.setCover(ImageUtils.getCoverFromURL(new URL(BEINSPORTS_HOST_URL.concat(xml_clip.getAttributeValue("videoImageSrc")))));

            String externalUrl = null;
            Integer bitrate = 0;
            for (org.jdom2.Element urlFileToDownload : xml_clip.getChild("videofiles").getChildren()) {
                if (Integer.valueOf(urlFileToDownload.getAttributeValue("bitrate")) > bitrate) {
                    bitrate = Integer.valueOf(urlFileToDownload.getAttributeValue("bitrate"));
                    externalUrl = urlFileToDownload.getAttributeValue("externalPath");
                }
            }

            Matcher m = SUBSTRING_VIDEO_URL_PATTERN.matcher(externalUrl);
            String vodUrl = null;
            if (m.find()) {
                for (String prefix : HTTP_VOD_PREFIX_URL) {
                    vodUrl = prefix.concat(m.group(1));
                    if (URLUtils.isAValidURL(vodUrl)) {
                        break;
                    } else {
                        vodUrl = null;
                    }
                }
            }

            item.setUrl((vodUrl != null) ? vodUrl : externalUrl);

        } catch (ParseException | IOException e) {
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
        String listingUrl = getListingUrl(podcast);

        if (!listingUrl.equals("")) {
            logger.debug("URL de signature : {}", listingUrl);
            return DigestUtils.generateMD5SignatureFromUrl(podcast.getUrl());
        } else {
            return "";
        }
    }

    private String getListingUrl(Podcast podcast) {
        String listingUrl = EPISODE_LISTING_URL;

        // Extraction de l'id de l'emission :
        Matcher m = IDBEINSPORT_PATTERN.matcher(podcast.getUrl());
        if (m.find()) {
            return listingUrl.replace("{idBeInSport}", m.group(1));
        } else {
            return "";
        }
    }

}
