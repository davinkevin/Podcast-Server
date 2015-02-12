package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lan.dk.podcastserver.service.WorkerService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


@Component
@Transactional
public class UpdatePodcastBusiness  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource PodcastBusiness podcastBusiness;
    @Resource ItemBusiness itemBusiness;

    @Resource @Qualifier("UpdateExecutor") AsyncTaskExecutor asyncExecutor;
    @Resource(name="Validator") Validator validator;
    @Resource WorkerService workerService;


    @Transactional(noRollbackFor=Exception.class)
    public void updatePodcast() {
        logger.info("Lancement de l'update");
        List<Podcast> podcasts = podcastBusiness.findByUrlIsNotNull();
        Updater updater;
        for (Podcast podcast : podcasts) {
            try {
                logger.info("Traitement du Podcast : " + podcast.toString());
                updater = workerService.getUpdaterByType(podcast);
                String signature = updater.generateSignature(podcast);
                if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                    updater.updateAndAddItems(podcast);


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

                updater = workerService.getUpdaterByType(podcast);
                String signature = updater.generateSignature(podcast);
                if ( signature != null && !signature.equals(podcast.getSignature()) ) {
                    podcast = updater.updateAndAddItems(podcast);


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
        Map<String, Future< Pair<Podcast, Set<Item>> > > podcastItemsToUpdate = new HashMap<>();

        List<Podcast> podcasts = podcastBusiness.findByUrlIsNotNull();
        logger.info("Traitement de {} podcasts", podcasts.size());
        for (Podcast podcast : podcasts) {
            try {
                final Updater updater = workerService.getUpdaterByType(podcast);
                podcastItemsToUpdate.put(podcast.getSignature(), asyncExecutor.submit(() -> updater.update(podcast)));
            } catch (Exception e) {
                logger.error("Error during signature of podcast {}", podcast.getTitle(), e);
                e.printStackTrace();
            }
        }

        logger.info("Traitement des ajouts sur {} podcasts", podcastItemsToUpdate.size());
        for (Map.Entry<String, Future<Pair<Podcast, Set<Item>>>> podcastAndItems : podcastItemsToUpdate.entrySet()) {
            try {
                String currentPodcastSignature = podcastAndItems.getKey();
                Pair<Podcast, Set<Item>> returnPaired = podcastAndItems.getValue().get(5, TimeUnit.MINUTES);
                if (!StringUtils.isEmpty(returnPaired.getKey().getSignature()) && !StringUtils.equals(currentPodcastSignature, returnPaired.getKey().getSignature())) {
                    podcastBusiness.update(
                            attachNewItemsToPodcast(returnPaired.getKey(), returnPaired.getValue())
                    );
                }
            } catch (Exception e) {
                logger.error("Error during update of podcast", e);
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
        Path fileToDelete;
        for (Item item : itemBusiness.findAllToDelete()) {
            fileToDelete = item.getLocalPath();
            logger.info("Suppression du fichier associé à l'item {} : {}", item.getId(), fileToDelete.toAbsolutePath().toString());
            try {
                Files.deleteIfExists(fileToDelete);
                itemBusiness.save(
                        item.setStatus("Deleted")
                            .setFileName(null)
                );
            } catch (IOException e) {
                logger.error("Error during suppression : ", e);
            }
        }
    }

    public Podcast attachNewItemsToPodcast(Podcast podcast, Set<Item> items) {
        
        items.stream()
                .filter(item -> !podcast.getItems().contains(item))
                .map(item -> item.setPodcast(podcast))
                .filter(item -> validator.validate(item).isEmpty())
                .forEach(item -> {
                    logger.debug("Add an item to {}", podcast.getTitle());
                    item.setPodcast(podcast);
                    podcast.getItems().add(item);
                    podcast.setLastUpdate(ZonedDateTime.now());
                });
        
        return podcast;
    }

    @PostConstruct
    public void resetItemWithIncorrectState() {
        logger.debug("Reset des Started et Paused");

        for (Item item : itemBusiness.findByStatus("Started", "Paused")) {
            itemBusiness.save(item.setStatus("Not Downloaded"));
        }
    }

}