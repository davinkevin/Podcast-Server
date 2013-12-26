package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.Updater;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Calendar;
import java.util.List;


@Component
public class UpdatePodcastBusiness implements ApplicationContextAware  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    PodcastBusiness podcastBusiness;

    @Autowired
    ItemBusiness itemBusiness;

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
        List<Podcast> podcasts = podcastBusiness.findAll();
        Document podcastXML = null;
        Updater updater;
        for (Podcast podcast : podcasts) {
            try {
                //podcastXML = jDomUtils.jdom2Parse(podcast.getUrl());
                updater = workerUtils.getUpdaterByType(podcast);
                String signature = updater.signaturePodcast(podcast);
                if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                    logger.debug("Traitement du Podcast : " + podcast.toString());
                    podcast = updater.updateFeed(podcast);


                    podcast.setLastUpdate(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
                    podcast.setSignature(signature);
                    podcast = podcastBusiness.update(podcast);
                    //podcast.setRssFeed(jDomUtils.podcastToXMLGeneric(podcast, serverURL));
                    podcast = podcastBusiness.update(podcast);

                }
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

    }

    public void updatePodcast(int id) {
        logger.debug("Lancement de l'update");
        Podcast podcast = podcastBusiness.findOne(id);
        Document podcastXML = null;
        Updater updater;
            try {
                logger.debug("Traitement du Podcast : " + podcast.toString());

                updater = workerUtils.getUpdaterByType(podcast);
                String signature = updater.signaturePodcast(podcast);
                if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                    podcast = updater.updateFeed(podcast);


                    podcast.setLastUpdate(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
                    podcast.setSignature(signature);
                    podcast = podcastBusiness.update(podcast);
                    //podcast.setRssFeed(jDomUtils.podcastToXMLGeneric(podcast, serverURL));
                    podcast = podcastBusiness.update(podcast);
                }

            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
    }

    public void forceUpdatePodcast (int id){
        logger.debug("Lancement de l'update forcé");
        Podcast podcast = podcastBusiness.findOne(id);
        podcast.setSignature("");
        podcastBusiness.update(podcast);
        this.updatePodcast(podcast.getId());
    }

    public void deleteOldEpisode() {
        logger.info("Suppression des anciens items");
        File fileToDelete = null;
        for (Item item : itemBusiness.findAllToDelete()) {
            fileToDelete = new File(rootFolder + item.getFileURI());
            logger.info("Suppression du fichier associé à l'item " + fileToDelete.getAbsolutePath());
            if (fileToDelete.exists() && fileToDelete.delete()) {
                logger.debug("Suppression effectuée");
                item.setStatus("Deleted");
                itemBusiness.save(item);
            }
        }
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        logger.debug("Initialisation du Contexte");
        this.context = applicationContext;
    }

    @PostConstruct
    public void resetItemWithIncorrectState() {

        logger.info("Reset des Started");

        for (Item item : itemBusiness.findByStatus("Started")) {
            itemBusiness.save(item.setStatus("Not Downloaded"));
        }

        logger.info("Reset des Paused");
        for (Item item : itemBusiness.findByStatus("Paused")) {
            itemBusiness.save(item.setStatus("Not Downloaded"));
        }
    }

}