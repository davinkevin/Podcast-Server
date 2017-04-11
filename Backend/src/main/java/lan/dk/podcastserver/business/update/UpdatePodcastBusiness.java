package lan.dk.podcastserver.business.update;

import javaslang.Tuple3;
import javaslang.collection.HashSet;
import javaslang.collection.Set;
import javaslang.control.Try;
import lan.dk.podcastserver.business.CoverBusiness;
import lan.dk.podcastserver.business.PodcastBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.selector.UpdaterSelector;
import lan.dk.podcastserver.manager.worker.updater.Updater;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.validation.Validator;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.function.Function.identity;

@Slf4j
@Component
public class UpdatePodcastBusiness  {

    private static final String WS_TOPIC_UPDATING = "/topic/updating";

    private TimeUnit timeUnit = TimeUnit.MINUTES;
    private Integer timeValue = 5;
    private @Getter ZonedDateTime lastFullUpdate;

    private final CoverBusiness coverBusiness;
    private final PodcastServerParameters podcastServerParameters;
    private final PodcastBusiness podcastBusiness;
    private final ItemRepository itemRepository;
    private final UpdaterSelector updaterSelector;
    private final SimpMessagingTemplate template;

    private final ThreadPoolTaskExecutor updateExecutor;
    private final ThreadPoolTaskExecutor manualExecutor;
    private final Validator validator;

    private AtomicBoolean isUpdating = new AtomicBoolean(false);

    @Autowired
    public UpdatePodcastBusiness(PodcastBusiness podcastBusiness, ItemRepository itemRepository, UpdaterSelector updaterSelector, SimpMessagingTemplate template, PodcastServerParameters podcastServerParameters, @Qualifier("UpdateExecutor") ThreadPoolTaskExecutor updateExecutor, @Qualifier("ManualUpdater") ThreadPoolTaskExecutor manualExecutor, @Qualifier("Validator") Validator validator, CoverBusiness coverBusiness) {
        this.podcastBusiness = podcastBusiness;
        this.itemRepository = itemRepository;
        this.updaterSelector = updaterSelector;
        this.template = template;
        this.podcastServerParameters = podcastServerParameters;
        this.updateExecutor = updateExecutor;
        this.manualExecutor = manualExecutor;
        this.validator = validator;
        this.coverBusiness = coverBusiness;
    }

    @Transactional
    public void updatePodcast() {
        updatePodcast(podcastBusiness.findByUrlIsNotNull(), updateExecutor);
        lastFullUpdate = ZonedDateTime.now();
    }

    @Transactional
    public void updatePodcast(UUID id) { updatePodcast(HashSet.of(podcastBusiness.findOne(id)), manualExecutor); }

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

    private void updatePodcast(Set<Podcast> podcasts, Executor selectedExecutor) {
        changeAndCommunicateUpdate(Boolean.TRUE);

        log.info("Update launch");
        log.info("About to update {} podcast(s)", podcasts.size());

        podcasts
            .map(podcast -> supplyAsync(() -> updaterSelector.of(podcast.getUrl()).update(podcast), selectedExecutor))
            .toStream()
            .map(this::wait)
            .filter(tuple -> tuple != Updater.NO_MODIFICATION_TUPLE)
            .peek(tuple -> changeAndCommunicateUpdate(Boolean.TRUE))
            .map(t -> attachNewItemsToPodcast(t._1(), t._2(), t._3()))
            .flatMap(identity())
            .forEach(coverBusiness::download);

        log.info("Fin du traitement des {} podcasts", podcasts.size());

        changeAndCommunicateUpdate(Boolean.FALSE);
    }

    private void changeAndCommunicateUpdate(Boolean isUpdating) {
        this.isUpdating.set(isUpdating);
        this.template.convertAndSend(WS_TOPIC_UPDATING, this.isUpdating.get());
    }

    private Tuple3<Podcast, Set<Item>, Predicate<Item>> wait(CompletableFuture<Tuple3<Podcast, Set<Item>, Predicate<Item>>> future) {
       return Try.of(() -> future.get(timeValue, timeUnit))
            .onFailure(e -> {
                log.error("Error during update", e);
                future.cancel(true);
            })
            .getOrElse(Updater.NO_MODIFICATION_TUPLE);
    }

    private Set<Item> attachNewItemsToPodcast(Podcast podcast, Set<Item> items, Predicate<Item> filter) {

        if (items == null || items.isEmpty() ) {
            log.info("Reset de la signature afin de forcer le prochain update de : {}", podcast.getTitle());
            podcastBusiness.save(podcast.setSignature(""));
            return HashSet.empty();
        }

        Set<Item> itemsToAdd = items
                .filter(filter)
                .map(item -> item.setPodcast(podcast))
                .filter(item -> validator.validate(item).isEmpty());

        if (itemsToAdd.isEmpty()) {
            return itemsToAdd;
        }

        itemsToAdd.peek(podcast::add).forEach(itemRepository::save);

        podcastBusiness.save(podcast.lastUpdateToNow());

        return itemsToAdd;
    }

    public void deleteOldEpisode() {
        log.info("Deletion of olds items");

        itemRepository
                .findAllToDelete(podcastServerParameters.limitDownloadDate())
                .toStream()
                .peek(item -> log.info("Deletion of file associated with item {} : {}", item.getId(), item.getLocalPath().toAbsolutePath()))
                .map(Item::deleteDownloadedFile)
                .forEach(itemRepository::save);
    }

    void setTimeOut(Integer timeValue, TimeUnit timeUnit) {
        this.timeValue = timeValue;
        this.timeUnit = timeUnit;
    }

    public void deleteOldCover() {
        log.info("Deletion of old covers item");
        itemRepository
                .findAllToDelete(podcastServerParameters.limitToKeepCoverOnDisk())
                .map(coverBusiness::getCoverPathOf)
                .forEach(p -> Try.of(() -> Files.deleteIfExists(p)));
    }

    public Integer getUpdaterActiveCount() {
        return updateExecutor.getActiveCount() + manualExecutor.getActiveCount();
    }

    @PostConstruct
    public void resetItemWithIncorrectState() {
        log.info("Reset des Started et Paused");

        itemRepository.findByStatus(Status.STARTED, Status.PAUSED)
                .map(i -> i.setStatus(Status.NOT_DOWNLOADED))
                .forEach(itemRepository::save);
    }
}
