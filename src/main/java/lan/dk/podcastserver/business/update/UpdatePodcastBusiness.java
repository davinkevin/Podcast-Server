package lan.dk.podcastserver.business.update;

import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.WorkerService;
import lan.dk.podcastserver.utils.facade.UpdateTuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.validation.Validator;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;


@Component
@Transactional
public class UpdatePodcastBusiness  {

    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private Integer timeValue = 5;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String WS_TOPIC_UPDATING = "/topic/updating";

    @Resource PodcastBusiness podcastBusiness;
    @Resource ItemRepository itemRepository;
    @Resource WorkerService workerService;
    @Resource SimpMessagingTemplate template;
    @Resource PodcastServerParameters podcastServerParameters;

    @Resource @Qualifier("UpdateExecutor") TaskExecutor updateExecutor;
    @Resource @Qualifier("ManualUpdater") TaskExecutor manualExecutor;
    @Resource(name="Validator") Validator validator;

    private AtomicBoolean isUpdating = new AtomicBoolean(false);

    public void updatePodcast() {
        updatePodcast(podcastBusiness.findByUrlIsNotNull(), updateExecutor);
    }
    public void updatePodcast(Integer id) { updatePodcast(Collections.singletonList(podcastBusiness.findOne(id)), manualExecutor); }

    public void forceUpdatePodcast (Integer id){
        logger.info("Lancement de l'update forcé");
        Podcast podcast = podcastBusiness.findOne(id);
        podcast.setSignature("");
        podcast = podcastBusiness.save(podcast);
        updatePodcast(podcast.getId());
    }

    @Transactional(noRollbackFor=Exception.class)
    private void updatePodcast(List<Podcast> podcasts, Executor selectedExecutor) {
        initUpdate();

        logger.info("Lancement de l'update");
        Set<Future<UpdateTuple<Podcast, Set<Item>, Predicate<Item>>>> futures = new HashSet<>();

        logger.info("Traitement de {} podcasts", podcasts.size());
        for (Podcast podcast : podcasts) {
            futures.add(
                    CompletableFuture
                            .supplyAsync(() -> workerService.updaterOf(podcast))
                            .thenApplyAsync(updater -> updater.update(podcast), selectedExecutor)
            );
        }
        logger.info("Attente des retours");

        handleUpdateTuple(futures);

        logger.info("Fin du traitement des {} podcasts", podcasts.size());
        finishUpdate();
    }

    private void finishUpdate() {
        changeAndCommunicateUpdate(Boolean.FALSE);
    }

    private void changeAndCommunicateUpdate(Boolean isUpdating) {
        this.isUpdating.set(isUpdating);
        this.template.convertAndSend(WS_TOPIC_UPDATING, this.isUpdating.get());
    }

    private void initUpdate() {
        changeAndCommunicateUpdate(Boolean.TRUE);
    }

    private void handleUpdateTuple(Set<Future<UpdateTuple<Podcast, Set<Item>, Predicate<Item>>>> futures) {
        try {
            for (Future<UpdateTuple<Podcast, Set<Item>, Predicate<Item>>> updateTupleFuture : futures) {
                UpdateTuple<Podcast, Set<Item>, Predicate<Item>> updateTuple = updateTupleFuture.get(timeValue, timeUnit);
                if (updateTuple != Updater.NO_MODIFICATION_TUPLE)
                    podcastBusiness.save(attachNewItemsToPodcast(updateTuple.first(), updateTuple.middle(), updateTuple.last()));
                changeAndCommunicateUpdate(Boolean.TRUE);
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            logger.error("Error during update", e);
        }
    }

    private Podcast attachNewItemsToPodcast(Podcast podcast, Set<Item> items, Predicate<Item> filter) {

        if (items == null || items.isEmpty() )
            return podcast;

        items.stream()
                .filter(filter)
                .map(item -> item.setPodcast(podcast))
                .filter(item -> validator.validate(item).isEmpty())
                .forEach(item -> {
                    logger.debug("Add an item to {}", podcast.getTitle());
                    podcast.getItems().add(item);
                    podcast.lastUpdateToNow();
                });

        return podcast;
    }

    public void deleteOldEpisode() {
        logger.info("Suppression des anciens items");
        Path fileToDelete;
        for (Item item : itemRepository.findAllToDelete(podcastServerParameters.limitDownloadDate())) {
            fileToDelete = item.getLocalPath();
            logger.info("Suppression du fichier associé à l'item {} : {}", item.getId(), fileToDelete.toAbsolutePath().toString());
            itemRepository.save( item.deleteDownloadedFile() );
        }
    }

    @PostConstruct
    public void resetItemWithIncorrectState() {
        logger.info("Reset des Started et Paused");

        StreamSupport
                .stream(itemRepository.findByStatus(Status.STARTED, Status.PAUSED).spliterator(), false)
                .map(item -> item.setStatus(Status.NOT_DOWNLOADED))
                .forEach(itemRepository::save);
    }

    public Boolean isUpdating() {
        return isUpdating.get();
    }

    public void setTimeOut(Integer timeValue, TimeUnit timeUnit) {
        this.timeValue = timeValue;
        this.timeUnit = timeUnit;
    }
}