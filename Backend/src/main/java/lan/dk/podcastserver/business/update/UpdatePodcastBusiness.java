package lan.dk.podcastserver.business.update;

import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lan.dk.podcastserver.utils.facade.UpdateTuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.validation.Validator;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Component
public class UpdatePodcastBusiness  {

    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private Integer timeValue = 5;
    private static final String WS_TOPIC_UPDATING = "/topic/updating";

    private final PodcastServerParameters podcastServerParameters;
    final PodcastBusiness podcastBusiness;
    final ItemRepository itemRepository;
    final UpdaterSelector updaterSelector;
    final SimpMessagingTemplate template;

    final TaskExecutor updateExecutor;
    final TaskExecutor manualExecutor;
    final Validator validator;

    private AtomicBoolean isUpdating = new AtomicBoolean(false);

    @Autowired
    public UpdatePodcastBusiness(PodcastBusiness podcastBusiness, ItemRepository itemRepository, UpdaterSelector updaterSelector, SimpMessagingTemplate template, PodcastServerParameters podcastServerParameters, @Qualifier("UpdateExecutor") TaskExecutor updateExecutor, @Qualifier("ManualUpdater") TaskExecutor manualExecutor, @Qualifier("Validator") Validator validator) {
        this.podcastBusiness = podcastBusiness;
        this.itemRepository = itemRepository;
        this.updaterSelector = updaterSelector;
        this.template = template;
        this.podcastServerParameters = podcastServerParameters;
        this.updateExecutor = updateExecutor;
        this.manualExecutor = manualExecutor;
        this.validator = validator;
    }

    @Transactional
    public void updatePodcast() {
        updatePodcast(podcastBusiness.findByUrlIsNotNull(), updateExecutor);
    }
    @Transactional
    public void updatePodcast(UUID id) { updatePodcast(Collections.singletonList(podcastBusiness.findOne(id)), manualExecutor); }

    @Transactional
    public void forceUpdatePodcast (UUID id){
        log.info("Lancement de l'update forcé");
        Podcast podcast = podcastBusiness.findOne(id);
        podcast.setSignature("");
        podcast = podcastBusiness.save(podcast);
        updatePodcast(podcast.getId());
    }

    public Boolean isUpdating() {
        return isUpdating.get();
    }

    private void updatePodcast(List<Podcast> podcasts, Executor selectedExecutor) {
        changeAndCommunicateUpdate(Boolean.TRUE);

        log.info("Update launch");
        log.info("About to update {} podcast(s)", podcasts.size());

        podcasts
                // Launch every update
                .stream()
                .map(podcast -> supplyAsync(() -> updaterSelector.of(podcast.getUrl()).update(podcast), selectedExecutor))
                .collect(toSet()) // Terminal operation forcing evaluation of each element upper in the stream
                // Get result of each update
                .stream()
                .map(this::wait)
                .filter(tuple -> tuple != Updater.NO_MODIFICATION_TUPLE)
                .peek(tuple -> changeAndCommunicateUpdate(Boolean.TRUE))
                .map(t -> attachNewItemsToPodcast(t.first(), t.middle(), t.last()))
                .forEach(podcastBusiness::save);

        log.info("Fin du traitement des {} podcasts", podcasts.size());

        changeAndCommunicateUpdate(Boolean.FALSE);
    }

    private void changeAndCommunicateUpdate(Boolean isUpdating) {
        this.isUpdating.set(isUpdating);
        this.template.convertAndSend(WS_TOPIC_UPDATING, this.isUpdating.get());
    }

    private UpdateTuple<Podcast, Set<Item>, Predicate<Item>> wait(CompletableFuture<UpdateTuple<Podcast, Set<Item>, Predicate<Item>>> future) {
        try {
            return future.get(timeValue, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error during update", e);
            future.cancel(true);
            return Updater.NO_MODIFICATION_TUPLE;
        }

    }

    private Podcast attachNewItemsToPodcast(Podcast podcast, Set<Item> items, Predicate<Item> filter) {

        if (items == null || items.isEmpty() ) {
            log.info("Reset de la signature afin de forcer le prochain update de : {}", podcast.getTitle());
            return podcast.setSignature("");
        }

        Set<Item> itemsToAdd = items.stream()
                .filter(filter)
                .map(item -> item.setPodcast(podcast))
                .filter(item -> validator.validate(item).isEmpty())
                .peek(item -> log.debug("Add Item {} to Podcast {}", item.getTitle(), podcast.getTitle()))
                .collect(toSet());

        if (itemsToAdd.isEmpty())
            return podcast;

        itemsToAdd.forEach(podcast::add);
        return podcast.lastUpdateToNow();
    }

    public void deleteOldEpisode() {
        log.info("Suppression des anciens items");
        Path fileToDelete;
        for (Item item : itemRepository.findAllToDelete(podcastServerParameters.limitDownloadDate())) {
            fileToDelete = item.getLocalPath();
            log.info("Suppression du fichier associé à l'item {} : {}", item.getId(), fileToDelete.toAbsolutePath().toString());
            itemRepository.save( item.deleteDownloadedFile() );
        }
    }

    @PostConstruct
    public void resetItemWithIncorrectState() {
        log.info("Reset des Started et Paused");

        StreamSupport
                .stream(itemRepository.findByStatus(Status.STARTED, Status.PAUSED).spliterator(), false)
                .map(item -> item.setStatus(Status.NOT_DOWNLOADED))
                .forEach(itemRepository::save);
    }

    void setTimeOut(Integer timeValue, TimeUnit timeUnit) {
        this.timeValue = timeValue;
        this.timeUnit = timeUnit;
    }
}