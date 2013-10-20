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

       Elements listingEpisodes = null;
       Document page = null;
        if (!podcast.getUrl().contains("front_tools")) { //Si c'est un lien direct vers la page de l'emmission, et donc le 1er Update
            podcast.setUrl(getRealPodcastURL(podcast.getUrl()));
        }

       try {
           for (Element episode : getListingEpisode(podcast.getUrl())) {
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
       } catch (MalformedURLException e) {
           e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
           logger.error("MalformedURLException :", e);
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

    private String getRealPodcastURL(String canalPlusDirectShowUrl) {
        if (canalPlusDirectShowUrl.contains("front_tools"))
            return canalPlusDirectShowUrl;

        int pid = 0;
        int ztid = 0;
        Document page = null;
        try {
            page = Jsoup.connect(canalPlusDirectShowUrl).get();
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
        return "http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid="+ pid +"&ztid="+ztid+"&nbPlusVideos0=1";
    }

    private Elements getListingEpisode (String canalPlusFrontToolsUrl) throws MalformedURLException{

        Document page = null;

        int nbPlusVideos = 0;

        Pattern p = Pattern.compile(".*nbPlusVideos([0-9])=1.*");
        Matcher m = p.matcher(canalPlusFrontToolsUrl);

        logger.debug("Parsing de l'url pour récupérer l'identifiant du tab");
        if (m.find()) {
            nbPlusVideos=Integer.parseInt(m.group(1));
            logger.debug("nbPlusVideos = " + nbPlusVideos);
        } else {
            throw new MalformedURLException("nbPlusVideos Introuvable pour le show " + canalPlusFrontToolsUrl);
        }

        try {
            page = Jsoup.connect(canalPlusFrontToolsUrl).get();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }
        return page.select("ul.features").get(nbPlusVideos).select("li");
    }
}
