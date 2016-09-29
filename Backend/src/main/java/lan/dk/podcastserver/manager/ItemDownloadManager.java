package lan.dk.podcastserver.manager;

import com.google.common.collect.Queues;
import javaslang.Tuple2;
import javaslang.collection.HashMap;
import javaslang.collection.HashSet;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.collection.Queue;
import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.selector.DownloaderSelector;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.CompletableFuture.runAsync;
import static lan.dk.podcastserver.manager.worker.selector.DownloaderSelector.NO_OP_DOWNLOADER;

@Slf4j
@Service
@Transactional
public class ItemDownloadManager {

    private static final String WS_TOPIC_WAITING_LIST = "/topic/waiting";

    private final SimpMessagingTemplate template;
    private final ItemRepository itemRepository;
    private final PodcastServerParameters podcastServerParameters;
    private final DownloaderSelector downloaderSelector;
    private final ThreadPoolTaskExecutor downloadExecutor;
    private final ReentrantLock mainLock = new ReentrantLock();

    private @Getter Queue<Item> waitingQueue = Queue.empty();
    private @Getter Map<Item, Downloader> downloadingQueue = HashMap.empty();

    @Autowired
    public ItemDownloadManager(SimpMessagingTemplate template, ItemRepository itemRepository, PodcastServerParameters podcastServerParameters, DownloaderSelector downloaderSelector, @Qualifier("DownloadExecutor") ThreadPoolTaskExecutor downloadExecutor) {
        this.template = template;
        this.itemRepository = itemRepository;
        this.podcastServerParameters = podcastServerParameters;
        this.downloaderSelector = downloaderSelector;
        this.downloadExecutor = downloadExecutor;

        Item.rootFolder = podcastServerParameters.getRootfolder();
    }

    /* GETTER & SETTER */
    public int getLimitParallelDownload() {
        return downloadExecutor.getCorePoolSize();
    }

    public void setLimitParallelDownload(Integer limitParallelDownload) {
        downloadExecutor.setCorePoolSize(limitParallelDownload);
        manageDownload();
    }

    public int getNumberOfCurrentDownload() {
        return downloadingQueue.size();
    }

    /* METHODS */
    private void manageDownload() {
        final ReentrantLock manageDownloadLock = this.mainLock;

        manageDownloadLock.lock();
        try {
            while (downloadingQueue.size() < downloadExecutor.getCorePoolSize() && !waitingQueue.isEmpty()) {
                Tuple2<Item, Queue<Item>> dequeue = waitingQueue.dequeue();
                Item currentItem = dequeue._1();
                this.waitingQueue = dequeue._2();
                if (!isStartedOrFinished(currentItem)) {
                    getDownloaderByTypeAndRun(currentItem);
                }
            }
        } finally {
            manageDownloadLock.unlock();
        }

        this.convertAndSendWaitingQueue();
    }

    private boolean isStartedOrFinished(Item currentItem) {
        return Status.STARTED == currentItem.getStatus() || Status.FINISH == currentItem.getStatus();
    }

    private void initDownload() {
        waitingQueue = waitingQueue.enqueueAll(
                HashSet.ofAll(itemRepository.findAllToDownload(podcastServerParameters.limitDownloadDate()))
                    .filter(item -> !waitingQueue.contains(item))
        );
    }

    public void launchDownload() {
        this.initDownload();
        this.manageDownload();
    }

    // Change status of all downloads :
    public void stopAllDownload() {
        downloadingQueue.values().forEach(Downloader::stopDownload);
    }

    public void pauseAllDownload() {
        downloadingQueue.values().forEach(Downloader::pauseDownload);
    }

    public void restartAllDownload() {
        List.ofAll(downloadingQueue.values())
                .filter(downloader -> Status.PAUSED == downloader.getItem().getStatus())
                .forEach(downloader -> runAsync(() -> getDownloaderByTypeAndRun(downloader.getItem())));
    }

    // Change State of id identified download
    public void stopDownload(UUID id) {
        getDownloaderOfItemWithId(id).forEach(Downloader::stopDownload);
    }

    public void pauseDownload(UUID id) {
        getDownloaderOfItemWithId(id).forEach(Downloader::pauseDownload);
    }

    private Option<Downloader> getDownloaderOfItemWithId(UUID id) {
        return downloadingQueue
                .find(es -> Objects.equals(es._1().getId(), id))
                .map(Tuple2::_2);
    }

    public void restartDownload(UUID id) {
        getDownloaderOfItemWithId(id)
                .map(Downloader::getItem)
                .forEach(this::getDownloaderByTypeAndRun);
    }

    public void toggleDownload(UUID id) {
        Item item = getDownloaderOfItemWithId(id)
                .map(Downloader::getItem)
                .getOrElse(Item.DEFAULT_ITEM);

        if (Status.PAUSED == item.getStatus()) {
            log.debug("restart du download");
            restartDownload(id);
        } else if (Status.STARTED == item.getStatus()) {
            log.debug("pause du download");
            pauseDownload(id);
        }
    }

    public void addItemToQueue(UUID id) {
        this.addItemToQueue(itemRepository.findOne(id));
    }

    void addItemToQueue(Item item) {

        if (waitingQueue.contains(item) || isInDownloadingQueue(item))
            return;

        waitingQueue = waitingQueue.enqueue(item);
        manageDownload();
    }

    @Transactional
    public void removeItemFromQueue(UUID id, Boolean stopItem) {
        Item item = itemRepository.findOne(id);
        this.removeItemFromQueue(item);

        if (stopItem)
            itemRepository.save(item.setStatus(Status.STOPPED));

        this.convertAndSendWaitingQueue();
    }

    private void removeItemFromQueue(Item item) {
        waitingQueue = waitingQueue.remove(item);
    }

    /* Helpers */
    public void removeACurrentDownload(Item item) {
        downloadingQueue = downloadingQueue.remove(item);
        manageDownload();
    }

    public Item getItemInDownloadingQueue(UUID id) {
        return this.downloadingQueue
                .keySet()
                .find(i -> Objects.equals(i.getId(), id))
                .getOrElse(() -> null);
    }

    private void getDownloaderByTypeAndRun(Item item) {
        if (isInDownloadingQueue(item)) { // case when the worker stay in the downloading queue
            log.debug("Start Item : " + item.getTitle());
            Downloader downloader = downloadingQueue.get(item).getOrElse(NO_OP_DOWNLOADER);
            downloader.restartDownload();
        } else { // Case when the worker totally end when paused, need to launch as new
            launchWithNewWorkerFrom(item);
        }
    }

    private void launchWithNewWorkerFrom(Item item) {
        Downloader worker = downloaderSelector
                .of(item.getUrl())
                .setItem(item)
                .setItemDownloadManager(this);

        downloadingQueue = downloadingQueue.put(item, worker);
        downloadExecutor.execute(worker);
    }

    public void resetDownload(Item item) {
        if (isInDownloadingQueue(item) && canBeReset(item)) {
            item.addATry();
            launchWithNewWorkerFrom(item);
        }
    }

    public void removeItemFromQueueAndDownload(Item itemToRemove) {
        //* If the download is started or paused : *//
        if (isInDownloadingQueue(itemToRemove)) {
            stopDownload(itemToRemove.getId());
        } else if (waitingQueue.contains(itemToRemove)) {
            removeItemFromQueue(itemToRemove);
        }
        this.convertAndSendWaitingQueue();
    }

    private void convertAndSendWaitingQueue() {
        this.template.convertAndSend(WS_TOPIC_WAITING_LIST, Queues.newConcurrentLinkedQueue(this.waitingQueue));
    }

    public boolean canBeReset(Item item) {
        return item.getNumberOfTry()+1 <= podcastServerParameters.getNumberOfTry();
    }

    public Boolean isInDownloadingQueue(Item item) {
        return downloadingQueue.containsKey(item);
    }

    public Set<Item> getItemsInDownloadingQueue() {
        return downloadingQueue.keySet().toJavaSet();
    }

    public void moveItemInQueue(UUID itemId, Integer position) {
        List<Item> copyWL = List.ofAll(waitingQueue);

        Item itemToMove = copyWL
            .find(item -> item.getId().equals(itemId))
            .getOrElseThrow(() -> new RuntimeException("Moving element in waiting list not authorized : Element wasn't in the list"));

        List<Item> reorderList = copyWL
                .removeFirst(item -> item.getId().equals(itemId))
                .insert(position, itemToMove);

        waitingQueue = Queue.ofAll(reorderList);

        convertAndSendWaitingQueue();
    }

    public void clearWaitingQueue() {
        waitingQueue = Queue.empty();
    }
}
