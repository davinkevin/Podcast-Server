package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lan.dk.podcastserver.utils.WorkerUtils;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executor;


@Component
@Transactional
public class UpdatePodcastBusiness implements ApplicationContextAware  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource PodcastBusiness podcastBusiness;
    @Resource ItemBusiness itemBusiness;
    @Resource WorkerUtils workerUtils;
    @Resource
    @Qualifier("UpdateExecutor")
    Executor asyncExecutor;

    @Value("${rootfolder}") protected String rootFolder;
    @Value("${serverURL}") protected String serverURL;
    @Value("${fileContainer}") protected String fileContainer;
    @Value("${numberofdaytodownload}") private int numberofdaytodownload;



    ApplicationContext context = null;

    @Transactional
    public void updatePodcast() {
        logger.info("Lancement de l'update");
        List<Podcast> podcasts = podcastBusiness.findByUrlIsNotNull();
        Document podcastXML = null;
        Updater updater;
        for (Podcast podcast : podcasts) {
            try {
                //podcastXML = jDomUtils.jdom2Parse(podcast.getUrl());
                updater = workerUtils.getUpdaterByType(podcast);
                String signature = updater.signaturePodcast(podcast);
                if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                    logger.info("Traitement du Podcast : " + podcast.toString());
                    updater.updateFeed(podcast);


                    podcast.setLastUpdate(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
                    podcast.setSignature(signature);
                    podcast = podcastBusiness.update(podcast);

                } else {
                    logger.info("Podcast non traité car signature identique : {}", podcast.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        /*while (true) {
            if (((ThreadPoolTaskExecutor) asyncExecutor).getThreadPoolExecutor().getActiveCount() != 0) {
                try {
                    logger.info("{} Thread Actif", ((ThreadPoolTaskExecutor) asyncExecutor).getThreadPoolExecutor().getActiveCount());
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else  {
                return;
            }
        }  */
    }

    @Transactional
    public void updatePodcast(int id) {
        logger.debug("Lancement de l'update");
        Podcast podcast = podcastBusiness.findOne(id);
        Document podcastXML = null;
        Updater updater;
            try {
                logger.info("Traitement du Podcast : " + podcast.toString());

                updater = workerUtils.getUpdaterByType(podcast);
                String signature = updater.signaturePodcast(podcast);
                if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                    podcast = updater.updateFeed(podcast);


                    podcast.setLastUpdate(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()));
                    podcast.setSignature(signature);
                    podcast = podcastBusiness.update(podcast);
                }

            } catch (Exception e) {
                logger.warn("Erreur d'update", e);
                //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
    }

    public void forceUpdatePodcast (int id){
        logger.info("Lancement de l'update forcé");
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

        logger.debug("Reset des Started");

        for (Item item : itemBusiness.findByStatus("Started")) {
            itemBusiness.save(item.setStatus("Not Downloaded"));
        }

        logger.debug("Reset des Paused");
        for (Item item : itemBusiness.findByStatus("Paused")) {
            itemBusiness.save(item.setStatus("Not Downloaded"));
        }
    }

}