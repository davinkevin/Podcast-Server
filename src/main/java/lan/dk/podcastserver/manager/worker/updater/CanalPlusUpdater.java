package lan.dk.podcastserver.manager.worker.updater;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("CanalPlusUpdater")
@Scope("prototype")
public class CanalPlusUpdater extends AbstractUpdater {

    public static final String CANALPLUS_PATTERN = "dd/MM/yyyy-HH:mm:ss";

    public Podcast updateAndAddItems(Podcast podcast) {

        // Si le bean est valide :
        getItems(podcast).stream()
                .filter(item -> !podcast.getItems().contains(item))
                .forEach(item -> {

                    item.setPodcast(podcast);
                    Set<ConstraintViolation<Item>> constraintViolations = validator.validate(item);
                    if (constraintViolations.isEmpty()) {
                        podcast.getItems().add(item);
                    } else {
                        logger.error(constraintViolations.toString());
                    }
                });

        logger.debug("Nombre d'episode : " + podcast.getItems().size());

        return podcast;
    }

    public Set<Item> getItems(Podcast podcast) {
        Document page;

        Set<Item> itemSet = new HashSet<>();

        try {
            page = Jsoup.connect(podcast.getUrl()).timeout(5000).get();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
            return itemSet;
        }

        // Si la page possède un planifier :
        if (!page.select(".planifier .cursorPointer").isEmpty() && itemSet.isEmpty()) { //Si c'est un lien direct vers la page de l'emmission, et donc le 1er Update
            itemSet = this.getSetItemToPodcastFromPlanifier(podcast.getUrl());
        }

        //Si c'est une page à onglet
        if (!page.select("#contenuOnglet .tab_content").isEmpty() && itemSet.isEmpty()) {
            itemSet = getItemSetFromOnglet(podcast.getUrl());
        }

        // Si pas d'autre solution, ou que l'url ne contient pas front_tools:
        if (!podcast.getUrl().contains("front_tools") && (itemSet.isEmpty())) { //Si c'est un lien direct vers la page de l'emmission, et donc le 1er Update
            itemSet = this.getSetItemToPodcastFromFrontTools(
                    this.getPodcastURLFromFrontTools(podcast.getUrl())
            );
        }

        // Si l'url est une front-tools et que la liste est encore vide :
        if (podcast.getUrl().contains("front_tools") && (itemSet.isEmpty())) { //Si c'est un lien direct vers la page de l'emmission, et donc le 1er Update
            itemSet = this.getSetItemToPodcastFromFrontTools(podcast.getUrl());
        }
        return itemSet;
    }



    public Podcast findPodcast(String url) {
        return null; // retourne un Podcast à partir de l'url fournie
    }

    @Override
    public String generateSignature(Podcast podcast) {
        Document page = null;

        try {
            page = Jsoup.connect(podcast.getUrl()).timeout(5000).get();
        } catch (IOException e) {
            logger.error("IOException :", e);
        }

        // Si la page possède un planifier :
        if (page != null) {
            if (!page.select(".planifier .cursorPointer").isEmpty()) { //Si c'est un lien direct vers la page de l'emmission, et donc le 1er Update
                return DigestUtils.generateMD5SignatureFromDOM(page.select(".planifier .cursorPointer").html());
            }
        }


        // Si pas d'autre solution, ou que l'url ne contient pas front_tools:
        if (!podcast.getUrl().contains("front_tools")) { //Si c'est un lien direct vers la page de l'emmission, et donc le 1er Update
            return DigestUtils.generateMD5SignatureFromUrl(this.getPodcastURLFromFrontTools(podcast.getUrl()));
        }

        // Si l'url est une front-tools et que la liste est encore vide :
        if (podcast.getUrl().contains("front_tools")) { //Si c'est un lien direct vers la page de l'emmission, et donc le 1er Update
            return DigestUtils.generateMD5SignatureFromUrl(podcast.getUrl());
        }
        return "";
    }


    /**
     * **** Partie spécifique au Front Tools ******
     */

    private String getPodcastURLFromFrontTools(String canalPlusDirectShowUrl) {
        if (canalPlusDirectShowUrl.contains("front_tools"))
            return canalPlusDirectShowUrl;

        int pid = 0;
        int ztid = 0;
        Document page = null;
        try {
            page = Jsoup.connect(canalPlusDirectShowUrl).timeout(5000).get();
            Pattern p = Pattern.compile(
                    "^loadVideoHistory\\('[0-9]*','[0-9]*','[0-9]*','([0-9]*)','([0-9]*)', '[0-9]*', '[^']*'\\);");
            //logger.debug(page.select("a[onclick^=loadVideoHistory]").first().attr("onclick"));
            Matcher m = p.matcher(page.select("a[onclick^=loadVideoHistory]").first().attr("onclick"));

            if (m.find()) {
                pid = Integer.parseInt(m.group(1));
                ztid = Integer.parseInt(m.group(2));
            }
            return "http://www.canalplus.fr/lib/front_tools/ajax/wwwplus_live_onglet.php?pid=" + pid + "&ztid=" + ztid + "&nbPlusVideos0=1";
        } catch (IOException e) {
            logger.error("IOException :", e);
        }
        return "";
    }

    private Elements getHTMLListingEpisodeFromFrontTools(String canalPlusFrontToolsUrl) throws MalformedURLException {

        Document page = null;

        int nbPlusVideos = 0;

        Pattern p = Pattern.compile(".*nbPlusVideos([0-9])=[1-9].*");
        Matcher m = p.matcher(canalPlusFrontToolsUrl);

        logger.debug("Parsing de l'url pour récupérer l'identifiant du tab");
        if (m.find()) {
            nbPlusVideos = Integer.parseInt(m.group(1));
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

    private Set<Item> getSetItemToPodcastFromFrontTools(String urlFrontTools) {
        Set<Item> itemSet = new HashSet<>();
        try {
            Integer idCanalPlusEpisode;
            for (Element episode : getHTMLListingEpisodeFromFrontTools(urlFrontTools)) {
                idCanalPlusEpisode = Integer.valueOf(episode.select("li._thumbs").first().id().replace("video_", ""));
                itemSet.add(getItemFromVideoId(idCanalPlusEpisode));
            }
            return itemSet;
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("MalformedURLException :", e);
        }
        return itemSet;
    }


    public Set<Item> getSetItemToPodcastFromPlanifier(String urlPodcast) {

        Set<Item> itemSet = new HashSet<>();
        Document page;
        try {
            page = Jsoup.connect(urlPodcast).get();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
            return itemSet;
        }
        //Pattern.compile("^loadVideoHistory\\('[0-9]*','[0-9]*','[0-9]*','([0-9]*)','([0-9]*)', '[0-9]*', '[^']*'\\);");
        Pattern idDuPodcastPatern = Pattern.compile(".*\\(([0-9]*).*\\);");
        Matcher matcher;
        for (Element cursorPointer : page.select(".planifier .cursorPointer")) {
            matcher = idDuPodcastPatern.matcher(cursorPointer.attr("onclick"));
            if (matcher.find()) {
                itemSet.add(getItemFromVideoId(Integer.valueOf(matcher.group(1))));
            }
        }
        return itemSet;
    }

    /**
     * Helper permettant de créer un item à partir d'un ID de Video Canal+
     *
     * @param idCanalPlusVideo
     * @return
     */
    private Item getItemFromVideoId(Integer idCanalPlusVideo) {
        Item currentEpisode = new Item();
        //currentEpisode.setTitle(episode.select("h4 a").first().text());
        org.jdom2.Document xmlAboutCurrentEpisode = null;
        try {
            xmlAboutCurrentEpisode = jDomUtils.jdom2Parse("http://service.canal-plus.com/video/rest/getVideos/cplus/" + idCanalPlusVideo);
        } catch (IOException | JDOMException e) {
            logger.error("IOException | JDOMException :", e);
            return new Item();
        }
        org.jdom2.Element xml_INFOS = xmlAboutCurrentEpisode.getRootElement().getChild("VIDEO").getChild("INFOS");
        org.jdom2.Element xml_MEDIA = xmlAboutCurrentEpisode.getRootElement().getChild("VIDEO").getChild("MEDIA");

        try {
            currentEpisode.setTitle(xml_INFOS.getChild("TITRAGE").getChildText("TITRE"))
                    .setPubdate(fromCanalPlus(xml_INFOS.getChild("PUBLICATION").getChildText("DATE"), xml_INFOS.getChild("PUBLICATION").getChildText("HEURE")))
                    .setCover(ImageUtils.getCoverFromURL(new URL(xml_MEDIA.getChild("IMAGES").getChildText("GRAND"))))
                    .setDescription(xml_INFOS.getChild("TITRAGE").getChildText("SOUS_TITRE"));

            if (xml_MEDIA.getChild("VIDEOS").getChildText("HLS") != null && StringUtils.isNotEmpty(xml_MEDIA.getChild("VIDEOS").getChildText("HLS"))) {
                currentEpisode.setUrl(URLUtils.getM3U8UrlFormMultiStreamFile(xml_MEDIA.getChild("VIDEOS").getChildText("HLS")));
            } else {
                currentEpisode.setUrl(xml_MEDIA.getChild("VIDEOS").getChildText("HD"));
            }

            logger.debug(currentEpisode.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("MalformedURLException pour l'item d'id Canal+ {}", idCanalPlusVideo, e);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
        }
        return currentEpisode;
    }

    private Set<Item> getItemSetFromOnglet(String url) {
        Document page;
        Set<Item> itemSet = new HashSet<>();
        try {
            page = Jsoup.connect(url).timeout(5000).get();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            logger.error("IOException :", e);
            return itemSet;
        }

        for (Element episode : page.select("#contenuOnglet li[id]")) {
            Integer idCanalPlusEpisode = Integer.valueOf(episode.id().replace("video_", ""));
            Item itemToAdd = getItemFromVideoId(idCanalPlusEpisode);
            itemSet.add(itemToAdd.setTitle(itemToAdd.getDescription()));
        }

        return itemSet;
    }

    public ZonedDateTime fromCanalPlus(String date, String heure) {
        LocalDateTime localDateTime = LocalDateTime.parse(date.concat("-").concat(heure), DateTimeFormatter.ofPattern(CANALPLUS_PATTERN));
        return ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Paris"));
    }
}
