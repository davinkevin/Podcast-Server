package lan.dk.podcastserver.controller;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lan.dk.podcastserver.service.api.ItemService;
import lan.dk.podcastserver.service.api.PodcastService;
import lan.dk.podcastserver.utils.DigestUtils;
import lan.dk.podcastserver.utils.WorkerUtils;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.Calendar;
import java.util.List;


@Controller
public class UpdatePodcastController implements ApplicationContextAware  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    PodcastService podcastService;

    @Autowired
    ItemService itemService;

    @Value("${rootfolder}")
    protected String rootFolder;

    @Value("${serverURL}")
    protected String serverURL;

    @Value("${fileContainer}")
    protected String fileContainer;

    @Value("${numberofdaytodownload}")
    private int numberofdaytodownload;

    @Autowired
    WorkerUtils workerUtils;

    ApplicationContext context = null;

    @Transactional
    public void updatePodcast() {
        logger.debug("Lancement de l'update");
        List<Podcast> podcasts = podcastService.findAll();
        Document podcastXML = null;
        Updater updater;
        for (Podcast podcast : podcasts) {
            String signature = DigestUtils.generateMD5Signature(podcast.getUrl());
            if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                try {
                    logger.debug("Traitement du Podcast : " + podcast.toString());
                    //podcastXML = jDomUtils.jdom2Parse(podcast.getUrl());

                    updater = workerUtils.getUpdaterByType(podcast);
                    podcast = updater.updateFeed(podcast);


                    podcast.setLastUpdate(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
                    podcast.setSignature(signature);
                    podcast = podcastService.update(podcast);
                    //podcast.setRssFeed(jDomUtils.podcastToXMLGeneric(podcast, serverURL));
                    podcast = podcastService.update(podcast);


                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }

    public void updatePodcast(int id) {
        logger.debug("Lancement de l'update");
        Podcast podcast = podcastService.findById(id);
        Document podcastXML = null;
        Updater updater;
        String signature = DigestUtils.generateMD5Signature(podcast.getUrl());
        if ( signature != null && !signature.equals(podcast.getSignature()) ) {
            try {
                logger.debug("Traitement du Podcast : " + podcast.toString());

                updater = workerUtils.getUpdaterByType(podcast);
                podcast = updater.updateFeed(podcast);


                podcast.setLastUpdate(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
                podcast.setSignature(signature);
                podcast = podcastService.update(podcast);
                //podcast.setRssFeed(jDomUtils.podcastToXMLGeneric(podcast, serverURL));
                podcast = podcastService.update(podcast);

            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public void forceUpdatePodcast (int id){
        logger.debug("Lancement de l'update forcé");
        Podcast podcast = podcastService.findById(id);
        podcast.setSignature("");
        podcastService.update(podcast);
        this.updatePodcast(podcast.getId());
    }

    public void deleteOldEpisode() {
        logger.info("Suppression des anciens items");
        File fileToDelete = null;
        for (Item item : itemService.findAllToDelete()) {
            fileToDelete = new File(rootFolder + item.getFileURI());
            logger.info("Suppression du fichier associé à l'item " + fileToDelete.getAbsolutePath());
            if (fileToDelete.exists() && fileToDelete.delete()) {
                logger.debug("Suppression effectuée");
                item.setStatus("Deleted");
                itemService.update(item);
            }
        }
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        logger.debug("Initialisation du Contexte");
        this.context = applicationContext;
    }

}