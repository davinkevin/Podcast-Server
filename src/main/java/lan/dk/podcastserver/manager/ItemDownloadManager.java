package lan.dk.podcastserver.manager;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.manager.worker.selector.DownloaderSelector;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service

public class ItemDownloadManager {

    private static final String WS_TOPIC_WAITINGLIST = "/topic/waiting";

    @Autowired SimpMessagingTemplate template;
    @Autowired ItemRepository itemRepository;
    @Autowired PodcastServerParameters podcastServerParameters;
    @Autowired DownloaderSelector downloaderSelector;

    private Queue<Item> waitingQueue = new ConcurrentLinkedQueue<>();
    private Map<Item, Downloader> downloadingQueue = new ConcurrentHashMap<>();
    private AtomicInteger numberOfCurrentDownload = new AtomicInteger(0);
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private Integer limitParallelDownload = 3;

    /* GETTER & SETTER */
    public int getLimitParallelDownload() {
        return podcastServerParameters.concurrentDownload();
    }

    public void changeLimitParallelsDownload(Integer limitParallelDownload) {

        boolean addToDownloadList = limitParallelDownload < getLimitParallelDownload();

        this.limitParallelDownload = limitParallelDownload;

        if (addToDownloadList && !isRunning.get())
            manageDownload();
    }

    public Queue<Item> getWaitingQueue() {
        return waitingQueue;
    }

    public Map<Item, Downloader> getDownloadingQueue() {
        return downloadingQueue;
    }

    public int getNumberOfCurrentDownload() {
        return numberOfCurrentDownload.get();
    }

    public String getRootfolder() {
        return podcastServerParameters.getRootfolder();
    }
    
    /* METHODS */
    private void manageDownload() {

        if (!isRunning.get()) {
            isRunning.set(true);
            Item currentItem;

            while (downloadingQueue.size() < this.limitParallelDownload && !waitingQueue.isEmpty()) {
                currentItem = this.getWaitingQueue().poll();
                if (!isStartedOrFinished(currentItem)) {
                     getDownloaderByTypeAndRun(currentItem);
                }
            }
            isRunning.set(false);
        }
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

    public void addItemToQueue(Item item) {

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
    public void addACurrentDownload() {
        this.numberOfCurrentDownload.incrementAndGet();
    }

    public void removeACurrentDownload(Item item) {
        this.numberOfCurrentDownload.decrementAndGet();
        this.downloadingQueue.remove(item);
        if (!isRunning.get())
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
            Downloader worker = downloaderSelector.of(item.getUrl());

            if (worker != null) {
                this.getDownloadingQueue().put(item, worker.setItem(item).setItemDownloadManager(this));
                new Thread(worker).start();
            }
        }
    }

    public void resetDownload(Item item) {
        if (downloadingQueue.containsKey(item) && canBeReseted(item)) {
            item.addATry();
            Downloader worker = downloaderSelector.of(item.getUrl());
            if (worker != null) {
                this.getDownloadingQueue().put(item, worker);
                new Thread(worker).start();
            }
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

    protected void convertAndSendWaitingQueue() {
        this.template.convertAndSend(WS_TOPIC_WAITINGLIST, this.waitingQueue);
    }


    public boolean canBeReseted(Item item) {
        return item.getNumberOfTry()+1 <= podcastServerParameters.numberOfTry();
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
    
    @PostConstruct
    public void postConstruct() throws URISyntaxException {
        Item.fileContainer = UriComponentsBuilder.fromUri(podcastServerParameters.fileContainer()).build().toUriString();
        Item.rootFolder = podcastServerParameters.rootFolder();
        limitParallelDownload = podcastServerParameters.concurrentDownload();
    }
}
