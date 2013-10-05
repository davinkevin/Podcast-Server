package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.DateUtils;
import lan.dk.podcastserver.utils.ImageUtils;
import lan.dk.podcastserver.utils.jDomUtils;
import org.jdom2.JDOMException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("CanalPlusUpdater")
@Scope("prototype")
public class CanalPlusUpdater extends AbstractUpdater {

   public Podcast updateFeed(Podcast podcast) {
        int pid = 0;
        int ztid = 0;

        Document page = null;
        try {
            page = Jsoup.connect(podcast.getUrl()).get();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }

        Pattern p = Pattern.compile(
                "^loadVideoHistory\\('[0-9]*','[0-9]*','[0-9]*','([0-9]*)','([0-9]*)', '[0-9]*', '[^']*'\\);");
        logger.debug(page.select("a[onclick^=loadVideoHistory]").first().attr("onclick"));
        Matcher m = p.matcher(page.select("a[onclick^=loadVideoHistory]").first().attr("onclick"));

        if (m.find()) {
            pid = Integer.parseInt(m.group(1));
            ztid = Integer.parseInt(m.group(2));
        }
        podcast.setUrl("http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid="+ pid +"&ztid="+ztid+"&nbPlusVideos0=1");
        //podcast.setSignature(DigestUtils.generateMD5Signature(podcast.getUrl()));
        try {
            page = Jsoup.connect(podcast.getUrl()).get();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }

        Elements listingEpisodes = page.select("ul.features").first().select("li");
        for (Element episode : listingEpisodes) {
            Item currentEpisode = new Item();
            currentEpisode.setTitle(episode.select("h4 a").first().text());
            org.jdom2.Document xmlAboutCurrentEpisode = null;
            try {
                xmlAboutCurrentEpisode = jDomUtils.jdom2Parse("http://service.canal-plus.com/video/rest/getVideos/cplus/" + episode.select("li._thumbs").first().id().replace("video_", ""));
            } catch (JDOMException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                logger.error("JDOMException :", e);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                logger.error("IOException :", e);
            }
            org.jdom2.Element xml_INFOS = xmlAboutCurrentEpisode.getRootElement().getChild("VIDEO").getChild("INFOS");
            org.jdom2.Element xml_MEDIA = xmlAboutCurrentEpisode.getRootElement().getChild("VIDEO").getChild("MEDIA");

            try {
                currentEpisode.setTitle(xml_INFOS.getChild("TITRAGE").getChildText("TITRE"));
                currentEpisode.setUrl(xml_MEDIA.getChild("VIDEOS").getChildText("HD"));
                currentEpisode.setPubdate(DateUtils.canalPlusDateToTimeStamp(xml_INFOS.getChild("PUBLICATION").getChildText("DATE"), xml_INFOS.getChild("PUBLICATION").getChildText("HEURE")));
                currentEpisode.setCover(ImageUtils.getCoverFromURL(new URL(xml_MEDIA.getChild("IMAGES").getChildText("GRAND"))));
                currentEpisode.setDescription(xml_INFOS.getChild("TITRAGE").getChildText("SOUS_TITRE"));
                //currentEpisode.setDescription((xml_INFOS.getChildText("DESCRIPTION").equals("")) ? xml_INFOS.getChild("TITRAGE").getChildText("SOUS_TITRE") : xml_INFOS.getChildText("DESCRIPTION"));

                if (!podcast.getItems().contains(currentEpisode)) {
                    currentEpisode.setPodcast(podcast);
                    podcast.getItems().add(currentEpisode);
                }
                logger.debug(currentEpisode.toString());
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                logger.error("ParseException :", e);
            } catch (MalformedURLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                logger.error("MalformedURLException :", e);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                logger.error("IOException :", e);
            }
        }

        podcast.setRssFeed(jDomUtils.podcastToXMLGeneric(podcast, this.getServerURL()));

        logger.debug(podcast.toString());
        logger.debug("Nombre d'episode : " + podcast.getItems().size());
        logger.debug(podcast.getRssFeed());


        return podcast;
    }

    public Podcast findPodcast(String url) {
        return null; // retourne un Podcast à partir de l'url fournie
    }
}
