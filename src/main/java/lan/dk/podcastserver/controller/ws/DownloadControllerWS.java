package lan.dk.podcastserver.controller.ws;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.util.Collection;

/**
 * Created by kevin on 27/06/2014.
 */
@Controller
public class DownloadControllerWS {

    @Resource
    ItemDownloadManager IDM;

    @SubscribeMapping("/waiting")
    private Collection<Item> waitingList() {
        return IDM.getWaitingQueue();
    }
    
    @SubscribeMapping("/download")
    private Collection<Item> downloadList() {
        return IDM.getItemsInDownloadingQueue();
    }
    
    @MessageMapping("/download/start")
    public void start(Item item) {
        IDM.restartDownload(item.getId());
    }
    
    @MessageMapping("/download/stop")
    public void stop(Item item) {
        IDM.stopDownload(item.getId());
    }

    @MessageMapping("/download/pause")
    public void pause(Item item) {
        IDM.pauseDownload(item.getId());
    }

    @MessageMapping("/download/toogle")
    public void toogle(Item item) {
        IDM.toogleDownload(item.getId());
    }
    
}
