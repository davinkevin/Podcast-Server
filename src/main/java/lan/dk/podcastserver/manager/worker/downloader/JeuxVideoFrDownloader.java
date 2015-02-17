package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.service.xml.JdomService;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevin on 29/11/14.
 */
@Component("JeuxVideoFrDownloader")
@Scope("prototype")
public class JeuxVideoFrDownloader extends HTTPDownloader {

    public static String XML_PREFIX_DESCRIPTOR_URL = "http://www.jeuxvideo.fr/api/tv/xml.php?player_generique=player_generique&id=%s";
    public static Pattern ID_JEUXVIDEOFR_PATTERN = Pattern.compile(".*-([0-9]*)\\..*");

    @Resource JdomService jdomService;
    
    public String jeuxVideoFrItemTempUrl = null;

    @Override
    public void run() {
        findJeuxVideoFrTmpUrl(item);
        if (!StringUtils.isEmpty(jeuxVideoFrItemTempUrl))
            super.run();
    }

    private Item findJeuxVideoFrTmpUrl(Item item) {

        Matcher m = ID_JEUXVIDEOFR_PATTERN.matcher(item.getUrl());
        if (!m.find() || m.group(1).equals("0")) {
            return item;
        }

        String idJeuxVideoFr = m.group(1);

        logger.debug("Id JeuxVideoFr : {}", idJeuxVideoFr);

        org.jdom2.Document xmlEpisode = null;
        try {
            xmlEpisode = jdomService.jdom2Parse(String.format(XML_PREFIX_DESCRIPTOR_URL, idJeuxVideoFr));
        } catch (JDOMException | IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return item;
        }

        Element xml_item = xmlEpisode.getRootElement().getChild("channel").getChild("item");

        if (StringUtils.isNotEmpty(xml_item.getChildText("url_video_hq"))) {
            jeuxVideoFrItemTempUrl = xml_item.getChildText("url_video_hq");
        } else if (StringUtils.isNotEmpty(xml_item.getChildText("url_video_sd"))) {
            jeuxVideoFrItemTempUrl = xml_item.getChildText("url_video_sd");
        } else if (StringUtils.isNotEmpty(xml_item.getChildText("url_video_3g"))) {
            jeuxVideoFrItemTempUrl = xml_item.getChildText("url_video_3g");
        }


        return item;
    }

    @Override
    public String getItemUrl() {
        return jeuxVideoFrItemTempUrl;
    }
}
