package lan.dk.podcastserver.manager;

import lan.dk.podcastserver.business.ItemBusiness;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.worker.downloader.Downloader;
import lan.dk.podcastserver.utils.WorkerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Controller
public class ItemDownloadManager implements ApplicationContextAware {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String WS_TOPIC_WAITINGLIST = "/topic/waitingList";

    //Représnetation de la fils d'attente
    private Queue<Item> waitingQueue = new ConcurrentLinkedQueue<Item>();
    //Représentation de la fils en cours de téléchargement
    private Map<Item, Downloader> downloadingQueue = new ConcurrentHashMap<Item, Downloader>();
    @Resource SimpMessagingTemplate template;

    @Autowired
    ItemBusiness itemBusiness;

    @Value("${concurrentDownload:3}")
    private int limitParallelDownload;

    @Value("${numberOfTry:10}")
    private int numberOfTry;


    @Value("${rootfolder:${catalina.home}/webapps/podcast/}")
    private String rootfolder;

    @Value("${serverURL:http://localhost:8080}")
    private String serverURL;

    @Value("${fileContainer:http://localhost:8080/podcast}")
    protected String fileContainer;



    @Autowired
    private WorkerUtils workerUtils;

    private AtomicInteger numberOfCurrentDownload = new AtomicInteger(0);
    private boolean isRunning = false;

    ApplicationContext context = null;

    /* GETTER & SETTER */
    public int getLimitParallelDownload() {
        return limitParallelDownload;
    }

    public void setLimitParallelDownload(int limitParallelDownload) {

        boolean addToDownloadList = limitParallelDownload < this.limitParallelDownload;

        this.limitParallelDownload = limitParallelDownload;

        if (addToDownloadList && !isRunning)
            manageDownload();
    }

    public ItemDownloadManager() {
    }

    public Queue<Item> getWaitingQueue() {
        return waitingQueue;
    }

    public Map<Item, Downloader> getDownloadingQueue() {
        return downloadingQueue;
    }

    public void setWaitingQueue(Queue<Item> waitingQueue) {
        this.waitingQueue = waitingQueue;
    }

    public void setDownloadingQueue(Map<Item, Downloader> downloadingQueue) {
        this.downloadingQueue = downloadingQueue;
    }

    public ItemDownloadManager(Queue<Item> waitingQueue) {

        this.waitingQueue = waitingQueue;
    }

    public int getNumberOfCurrentDownload() {
        return numberOfCurrentDownload.get();
    }

    public void setNumberOfCurrentDownload(int numberOfCurrentDownload) {
        this.numberOfCurrentDownload.set(numberOfCurrentDownload);
    }


    public String getRootfolder() {
        return rootfolder;
    }

    public void setRootfolder(String rootfolder) {
        this.rootfolder = rootfolder;
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public String getFileContainer() {
        return fileContainer;
    }

    public void setFileContainer(String fileContainer) {
        this.fileContainer = fileContainer;
    }

    /* METHODS */
    private void manageDownload() {

        if (!isRunning) {
            isRunning = true;
            Item currentItem = null;

            while (downloadingQueue.size() < this.limitParallelDownload && !waitingQueue.isEmpty()) {
                currentItem = this.getWaitingQueue().poll();
                if ( !"Started".equals(currentItem.getStatus()) && !"Finish".equals(currentItem.getStatus()) ) {
                     getDownloaderByTypeAndRun(currentItem);
                }
            }
            isRunning = false;
        }
        this.convertAndSendWaitingQueue();
    }

    private void initDownload() {
        for(Item item : itemBusiness.findAllToDownload()) {
            if (!waitingQueue.contains(item)) {
                waitingQueue.add(item);
            }
        }
    }

    public void launchDownload() {
        this.initDownload();
        this.manageDownload();
    }


    // Change status of all downloads :
    public void stopAllDownload() {
        for (Downloader downloader : downloadingQueue.values()) {
            downloader.stopDownload();
        }
    }

    public void pauseAllDownload() {
        for (Downloader downloader : downloadingQueue.values()) {
            downloader.pauseDownload();
        }
    }

    public void restartAllDownload() {
        for (Downloader downloader : downloadingQueue.values()) {
            if (downloader.getItem().getStatus().equals("Paused")) {
                getDownloaderByTypeAndRun(downloader.getItem());
            }
        }
    }

    // Change State of id identified download
    public void stopDownload(int id) {
        downloadingQueue.get(getItemInDownloadingQueue(id)).stopDownload();
    }

    public void pauseDownload(int id) {
        Item item = getItemInDownloadingQueue(id);
        downloadingQueue.get(item).pauseDownload();
    }

    public void restartDownload(int id) {
        getDownloaderByTypeAndRun(downloadingQueue.get(getItemInDownloadingQueue(id)).getItem());
    }

    public void toogleDownload(int id) {
        Item item = getItemInDownloadingQueue(id);
        if (item.getStatus().equals("Paused")) {
            logger.debug("restart du download");
            restartDownload(id);
        } else if (item.getStatus().equals("Started")) {
            logger.debug("pause du download");
            pauseDownload(id);
        }
    }

    public void addItemToQueue(int id) {
        this.addItemToQueue(itemBusiness.findOne(id));
        //this.convertAndSendWaitingQueue();
    }

    public void addItemToQueue(Item item) {

        if (waitingQueue.contains(item) || downloadingQueue.containsKey(item))
            return;

        waitingQueue.add(item);
        manageDownload();
    }

    public void removeItemFromQueue(int id, Boolean stopItem) {
        Item item = itemBusiness.findOne(id);
        this.removeItemFromQueue(item);

        if (stopItem)
            itemBusiness.save(item.setStatus("Stopped"));

        this.convertAndSendWaitingQueue();
    }

    public void removeItemFromQueue(Item item) {
        waitingQueue.remove(item);
    }


    /* Helpers */
    public void addACurrentDownload() {
        this.numberOfCurrentDownload.incrementAndGet();
    }

    public void removeACurrentDownload(Item item) {
        this.numberOfCurrentDownload.decrementAndGet();
        this.downloadingQueue.remove(item);
        if (!isRunning)
            manageDownload();
    }

    public Item getItemInDownloadingQueue(int id) {
        for (Item item : downloadingQueue.keySet()) {
            if (item.getId()== id) {
                return item;
            }
        }
        return null;
    }

    private void getDownloaderByTypeAndRun(Item item) {
        if (downloadingQueue.containsKey(item)) { // Cas ou le Worker se met en pause et reste en mémoire // dans la DownloadingQueue
            Downloader downloader = downloadingQueue.get(item);
            downloader.startDownload();
        } else { // Cas ou le Worker se coupe pour la pause et nécessite un relancement
            Downloader worker = workerUtils.getDownloaderByType(item);

            if (worker != null) {
                this.getDownloadingQueue().put(item, worker);
                new Thread((Runnable) worker).start();
            }
        }
    }

    public void resetDownload(Item item) {
        if (downloadingQueue.containsKey(item) && canBeReseted(item)) {
            item.addATry();
            Downloader worker = workerUtils.getDownloaderByType(item);
            if (worker != null) {
                this.getDownloadingQueue().put(item, worker);
                new Thread((Runnable) worker).start();
            }
        }
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        logger.debug("Initialisation du Contexte");
        this.context = applicationContext;
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
        return item.getNumberOfTry()+1 <= numberOfTry;
    }

    public Boolean isInDownloadingQueue(Item item) {
        return downloadingQueue.containsKey(item);
    }

}
