package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lan.dk.podcastserver.service.WorkerService;
import lan.dk.podcastserver.utils.facade.UpdateTuple;
import org.apache.commons.lang3.StringUtils;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;


@Component
@Transactional
public class UpdatePodcastBusiness  {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource PodcastBusiness podcastBusiness;
    @Resource ItemBusiness itemBusiness;

    @Resource @Qualifier("UpdateExecutor") AsyncTaskExecutor asyncExecutor;
    @Resource(name="Validator") Validator validator;
    @Resource WorkerService workerService;
    public static final ExecutorService SINGLE_THREAD_EXECUTOR = Executors.newSingleThreadExecutor();


    @Transactional(noRollbackFor=Exception.class)
    public void updatePodcast(Integer id) {
        logger.debug("Lancement de l'update");
        final Podcast podcast = podcastBusiness.findOne(id);
        Updater updater;
            try {
                logger.debug("Traitement du Podcast singulier {}", podcast.getTitle());

                updater = workerService.updaterOf(podcast);
                Future <UpdateTuple<Podcast, Set<Item>, Predicate<Item>>> updateTupleFuture = SINGLE_THREAD_EXECUTOR.submit(() -> updater.update(podcast));
                UpdateTuple<Podcast, Set<Item>, Predicate<Item>> updateTuple = updateTupleFuture.get(5, TimeUnit.MINUTES);

                String signature = updateTuple.first().getSignature();
                if ( StringUtils.equals(podcast.getSignature(), signature)) {
                    podcastBusiness.update(
                            attachNewItemsToPodcast(updateTuple.first(), updateTuple.middle(), updateTuple.last())
                    );
                }

            } catch (Exception e) {
                logger.warn("Erreur d'update singulier", e);
            }
        logger.debug("Fin du traitement singulier de {}", podcast.getTitle());
    }

    public void updateAsyncPodcast() {
        logger.info("Lancement de l'update");
        Map<String, Future< UpdateTuple<Podcast, Set<Item>, Predicate<Item>> > > podcastItemsToUpdate = new HashMap<>();

        List<Podcast> podcasts = podcastBusiness.findByUrlIsNotNull();
        logger.info("Traitement de {} podcasts", podcasts.size());
        for (Podcast podcast : podcasts) {
            try {
                final Updater updater = workerService.updaterOf(podcast);
                podcastItemsToUpdate.put(podcast.getSignature(), asyncExecutor.submit(() -> updater.update(podcast)));
            } catch (Exception e) {
                logger.error("Error during signature of podcast {}", podcast.getTitle(), e);
            }
        }

        logger.info("Traitement des ajouts sur {} podcasts", podcastItemsToUpdate.size());
        for (Map.Entry<String, Future<UpdateTuple<Podcast, Set<Item>, Predicate<Item>>>> podcastAndItems : podcastItemsToUpdate.entrySet()) {
            try {
                String currentPodcastSignature = podcastAndItems.getKey();
                UpdateTuple<Podcast, Set<Item>, Predicate<Item>> returnValue = podcastAndItems.getValue().get(5, TimeUnit.MINUTES);
                if (!StringUtils.isEmpty(returnValue.first().getSignature()) && !StringUtils.equals(currentPodcastSignature, returnValue.first().getSignature())) {
                    podcastBusiness.update(
                            attachNewItemsToPodcast(returnValue.first(), returnValue.middle(), returnValue.last())
                    );
                }
            } catch (Exception e) {
                logger.error("Error during update of podcast", e);
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
                        item.setStatus(Status.DELETED)
                            .setFileName(null)
                );
            } catch (IOException e) {
                logger.error("Error during suppression : ", e);
            }
        }
    }

    public Podcast attachNewItemsToPodcast(Podcast podcast, Set<Item> items, Predicate<Item> filter) {
        
        if (items == null || items.isEmpty() )
            return podcast;
        
        items.stream()
                .filter(filter)
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

        for (Item item : itemBusiness.findByStatus(Status.STARTED, Status.PAUSED)) {
            itemBusiness.save(item.setStatus(Status.NOT_DOWNLOADED));
        }
    }

}