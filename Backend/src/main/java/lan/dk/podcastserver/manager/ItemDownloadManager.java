package lan.dk.podcastserver.manager;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.selector.DownloaderSelector;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.StreamSupport;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@Transactional
public class ItemDownloadManager {

    private static final String WS_TOPIC_WAITINGLIST = "/topic/waiting";

    private final SimpMessagingTemplate template;
    private final ItemRepository itemRepository;
    private final PodcastServerParameters podcastServerParameters;
    private final DownloaderSelector downloaderSelector;
    private final ThreadPoolTaskExecutor downloadExecutor;
    private final ReentrantLock mainLock = new ReentrantLock();

    @Autowired
    public ItemDownloadManager(SimpMessagingTemplate template, ItemRepository itemRepository, PodcastServerParameters podcastServerParameters, DownloaderSelector downloaderSelector, @Qualifier("DownloadExecutor") ThreadPoolTaskExecutor downloadExecutor) {
        this.template = template;
        this.itemRepository = itemRepository;
        this.podcastServerParameters = podcastServerParameters;
        this.downloaderSelector = downloaderSelector;
        this.downloadExecutor = downloadExecutor;

        Item.rootFolder = podcastServerParameters.getRootfolder();
    }

    private Queue<Item> waitingQueue = Queues.newConcurrentLinkedQueue();
    private Map<Item, Downloader> downloadingQueue = Maps.newConcurrentMap();

    /* GETTER & SETTER */
    public int getLimitParallelDownload() {
        return downloadExecutor.getCorePoolSize();
    }

    public void setLimitParallelDownload(Integer limitParallelDownload) {
        downloadExecutor.setCorePoolSize(limitParallelDownload);
        manageDownload();
    }

    public Queue<Item> getWaitingQueue() {
        return waitingQueue;
    }

    public Map<Item, Downloader> getDownloadingQueue() {
        return downloadingQueue;
    }

    public int getNumberOfCurrentDownload() {
        return downloadingQueue.size();
    }

    public String getRootfolder() {
        return podcastServerParameters.getRootfolder().toString();
    }

    /* METHODS */
    private void manageDownload() {
        final ReentrantLock manageDownloadLock = this.mainLock;

        manageDownloadLock.lock();
        try {
            while (downloadingQueue.size() < downloadExecutor.getCorePoolSize() && !waitingQueue.isEmpty()) {
                Item currentItem = this.getWaitingQueue().poll();
                if (!isStartedOrFinished(currentItem)) {
                    getDownloaderByTypeAndRun(currentItem);
                }
            }
        } finally {
            manageDownloadLock.unlock();
        }
        log.info("Call to convert and Send waiting queue {}", waitingQueue);
        this.convertAndSendWaitingQueue();
    }

    private boolean isStartedOrFinished(Item currentItem) {
        return Status.STARTED == currentItem.getStatus() || Status.FINISH == currentItem.getStatus();
    }

    private void initDownload() {
        StreamSupport
                .stream(itemRepository.findAllToDownload(podcastServerParameters.limitDownloadDate()).spliterator(), false)
                .filter(item -> !waitingQueue.contains(item))
                .forEach(waitingQueue::add);
    }

    public void launchDownload() {
        this.initDownload();
        this.manageDownload();
    }


    // Change status of all downloads :
    public void stopAllDownload() {
        downloadingQueue.values()
                .stream().collect(toList())
                .forEach(Downloader::stopDownload);
    }

    public void pauseAllDownload() {
        downloadingQueue.values().forEach(Downloader::pauseDownload);
    }

    public void restartAllDownload() {
        downloadingQueue.values()
                .stream()
                .filter(downloader -> Status.PAUSED == downloader.getItem().getStatus())
                .forEach(downloader -> runAsync(() -> getDownloaderByTypeAndRun(downloader.getItem())));
    }

    // Change State of id identified download
    public void stopDownload(UUID id) {
        getDownloaderOfItemWithId(id).ifPresent(Downloader::stopDownload);
    }

    public void pauseDownload(UUID id) {
        getDownloaderOfItemWithId(id).ifPresent(Downloader::pauseDownload);
    }

    private Optional<Downloader> getDownloaderOfItemWithId(UUID id) {
        return downloadingQueue
                .entrySet()
                .stream()
                .filter(es -> Objects.equals(es.getKey().getId(), id))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    public void restartDownload(UUID id) {
        getDownloaderOfItemWithId(id)
                .map(Downloader::getItem)
                .ifPresent(this::getDownloaderByTypeAndRun);
    }

    public void toogleDownload(UUID id) {
        Item item = getDownloaderOfItemWithId(id)
                .map(Downloader::getItem)
                .orElse(Item.DEFAULT_ITEM);

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

        if (waitingQueue.contains(item) || downloadingQueue.containsKey(item))
            return;

        waitingQueue.add(item);
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
        waitingQueue.remove(item);
    }


    /* Helpers */
    public void removeACurrentDownload(Item item) {
        this.downloadingQueue.remove(item);
        manageDownload();
    }

    public Item getItemInDownloadingQueue(UUID id) {
        return this.downloadingQueue
                .keySet()
                .stream()
                .filter(i -> Objects.equals(i.getId(), id))
                .findFirst()
                .orElse(null);
    }

    private void getDownloaderByTypeAndRun(Item item) {
        if (downloadingQueue.containsKey(item)) { // Cas ou le Worker se met en pause et reste en mémoire // dans la DownloadingQueue
            log.debug("Start Item : " + item.getTitle());
            Downloader downloader = downloadingQueue.get(item);
            downloader.startDownload();
        } else { // Cas ou le Worker se coupe pour la pause et nécessite un relancement
            Downloader worker = downloaderSelector
                    .of(item.getUrl())
                    .setItem(item)
                    .setItemDownloadManager(this);
            this.getDownloadingQueue().put(item, worker);
            downloadExecutor.execute(worker);
        }
    }

    public void resetDownload(Item item) {
        if (downloadingQueue.containsKey(item) && canBeReseted(item)) {
            item.addATry();
            Downloader worker = downloaderSelector
                    .of(item.getUrl())
                    .setItem(item)
                    .setItemDownloadManager(this);
            this.getDownloadingQueue().put(item, worker);
            downloadExecutor.execute(worker);
        }
    }

    public void removeItemFromQueueAndDownload(Item itemToDelete) {
        //* Si le téléchargement est en cours ou en attente : *//
        if (this.getDownloadingQueue().containsKey(itemToDelete)) {
            this.stopDownload(itemToDelete.getId());
        } else if (this.getWaitingQueue().contains(itemToDelete)) {
            this.removeItemFromQueue(itemToDelete);
        }
        this.convertAndSendWaitingQueue();
    }

    private void convertAndSendWaitingQueue() {
        this.template.convertAndSend(WS_TOPIC_WAITINGLIST, this.waitingQueue);
    }


    public boolean canBeReseted(Item item) {
        return item.getNumberOfTry()+1 <= podcastServerParameters.getNumberOfTry();
    }

    public Boolean isInDownloadingQueue(Item item) {
        return downloadingQueue.containsKey(item);
    }

    public Set<Item> getItemsInDownloadingQueue() {
        return downloadingQueue.keySet();
    }

    public void moveItemInQueue(UUID itemId, Integer position) {
        List<Item> copyOfWaitingList = Arrays.asList(waitingQueue.toArray(new Item[waitingQueue.size()]));
        List<Item> aItemList = new ArrayList<>();

        copyOfWaitingList.stream().forEach(aItemList::add);

        Optional<Item> movingItem = aItemList.stream()
                .filter(item -> item.getId().equals(itemId)).findFirst();

        if (!movingItem.isPresent()) {
            log.error("Moving element in waiting list not authorized : Element wasn't in the list");
            return;
        }

        aItemList.removeIf(item -> item.getId().equals(itemId));
        aItemList.add(position, movingItem.get());

        waitingQueue.clear();
        waitingQueue.addAll(aItemList);

        convertAndSendWaitingQueue();
    }
}
