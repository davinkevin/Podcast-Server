package lan.dk.podcastserver.controller.ws;

import com.github.davinkevin.podcastserver.entity.Item;
import com.github.davinkevin.podcastserver.manager.ItemDownloadManager;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.util.Collection;

/**
 * Created by kevin on 27/06/2014.
 */
@Controller
public class DownloadControllerWS {

    private final ItemDownloadManager IDM;

    @java.beans.ConstructorProperties({"IDM"})
    public DownloadControllerWS(ItemDownloadManager IDM) {
        this.IDM = IDM;
    }

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
