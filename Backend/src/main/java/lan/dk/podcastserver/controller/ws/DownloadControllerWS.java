package lan.dk.podcastserver.controller.ws;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Collection;

/**
 * Created by kevin on 27/06/2014.
 */
@Controller
@RequiredArgsConstructor
public class DownloadControllerWS {

    private final ItemDownloadManager IDM;

    @SubscribeMapping("/waiting")
    public Collection<Item> waitingList() {
        return IDM.getWaitingQueue().toJavaList();
    }
    
    @SubscribeMapping("/download")
    public Iterable<Item> downloadList() {
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
        IDM.toggleDownload(item.getId());
    }
    
}
