package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lan.dk.podcastserver.utils.WorkerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;


@Component
@Transactional
public class UpdatePodcastBusiness  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource PodcastBusiness podcastBusiness;
    @Resource ItemBusiness itemBusiness;
    @Resource WorkerUtils workerUtils;

    @Resource
    @Qualifier("UpdateExecutor") AsyncTaskExecutor asyncExecutor;
    @Resource(name="Validator") Validator validator;

    @Value("${rootfolder:${catalina.home}/webapp/podcast/}") protected String rootFolder;


    @Transactional(noRollbackFor=Exception.class)
    public void updatePodcast() {
        logger.info("Lancement de l'update");
        List<Podcast> podcasts = podcastBusiness.findByUrlIsNotNull();
        Updater updater;
        for (Podcast podcast : podcasts) {
            try {
                logger.info("Traitement du Podcast : " + podcast.toString());
                updater = workerUtils.getUpdaterByType(podcast);
                String signature = updater.signaturePodcast(podcast);
                if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                    updater.updateFeed(podcast);


                    podcast.setLastUpdate(ZonedDateTime.now());
                    podcast.setSignature(signature);
                    podcastBusiness.update(podcast);

                } else {
                    logger.info("Podcast non traité car signature identique : {}", podcast.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        logger.info("Fin du traitement des {} podcasts", podcasts.size());
    }

    @Transactional(noRollbackFor=Exception.class)
    public void updatePodcast(int id) {
        logger.debug("Lancement de l'update");
        Podcast podcast = podcastBusiness.findOne(id);
        Updater updater;
            try {
                logger.info("Traitement du Podcast : " + podcast.toString());

                updater = workerUtils.getUpdaterByType(podcast);
                String signature = updater.signaturePodcast(podcast);
                if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                    podcast = updater.updateFeed(podcast);


                    podcast.setLastUpdate(ZonedDateTime.now());
                    podcast.setSignature(signature);
                    podcastBusiness.update(podcast);
                }

            } catch (Exception e) {
                logger.warn("Erreur d'update", e);
                //e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
    }

    public void updateAsyncPodcast() {
        logger.info("Lancement de l'update");
        Map<Podcast, Future<Set<Item>> > podcastItemsToUpdate = new HashMap<>();

        List<Podcast> podcasts = podcastBusiness.findByUrlIsNotNull();
        Updater updater;
        for (Podcast podcast : podcasts) {
            try {
                logger.info("Ajout du podcast {} à l'executor", podcast.toString());
                updater = workerUtils.getUpdaterByType(podcast);
                String signature = updater.signaturePodcast(podcast);
                if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                    podcast.setSignature(signature);
                    podcast.setLastUpdate(ZonedDateTime.now());

                    final Updater finalUpdater = updater;
                    podcastItemsToUpdate.put(podcast, asyncExecutor.submit(() -> finalUpdater.updateFeedAsync(podcast)));


                } else {
                    logger.info("Podcast non traité car signature identique : {}", podcast.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        for (Map.Entry<Podcast, Future<Set<Item>>> podcastAndItems : podcastItemsToUpdate.entrySet()) {
            try {
                podcastBusiness.update(attachNewItemsToPodcast(podcastAndItems.getKey(), podcastAndItems.getValue().get()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        logger.info("Fin du traitement des {} podcasts", podcasts.size());
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
        File fileToDelete;
        for (Item item : itemBusiness.findAllToDelete()) {
            fileToDelete = new File(rootFolder + item.getFileURI());
            logger.info("Suppression du fichier associé à l'item " + fileToDelete.getAbsolutePath());
            if (fileToDelete.exists() && fileToDelete.delete()) {
                logger.debug("Suppression effectuée");
                item.setStatus("Deleted")
                        .setLocalUri(null)
                        .setLocalUrl(null);
                itemBusiness.save(item);
            }
        }
    }

    public Podcast attachNewItemsToPodcast(Podcast podcast, Set<Item> items) {
        items.stream()
                .filter(item -> !podcast.getItems().contains(item))
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