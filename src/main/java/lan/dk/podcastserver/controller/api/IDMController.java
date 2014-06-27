package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Queue;
import java.util.Set;

/**
 * Created by kevin on 26/12/2013.
 */
@Controller
@RequestMapping("/api/task/downloadManager")
public class IDMController {

    @Autowired
    ItemDownloadManager IDM;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping(value="/queue", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Queue<Item> getDownloadList () {
        return IDM.getWaitingQueue();
    }

    @RequestMapping(value="/downloading", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Set<Item> getDownloadingList () {
        return IDM.getDownloadingQueue().keySet();
    }

    @RequestMapping(value="/downloading/{id:[\\d]+}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Item getDownloadingList (@PathVariable int id) {
        return IDM.getItemInDownloadingQueue(id);
    }

    @RequestMapping(value="/current", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public int getNumberOfCurrentDownload () {
        return IDM.getNumberOfCurrentDownload();
    }

    @RequestMapping(value="/limit", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public int setLimitParallelDownload () {
        return IDM.getLimitParallelDownload();
    }

    @RequestMapping(value="/limit", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public void setLimitParallelDownload (@RequestBody int setLimitParallelDownload) {
        IDM.setLimitParallelDownload(setLimitParallelDownload);
    }

    @RequestMapping(value="/launch", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void launchDownload() {

        IDM.launchDownload();
    }

    @RequestMapping(value="/downloading/{id:[\\d]+}", method = RequestMethod.POST, produces = "application/json")
    @ResponseBody
    public void changeStatusDownload (@RequestBody String status, @PathVariable(value = "id") int id) {
        logger.debug("id : " + id + "; status : " + status);
    }

    // Action on ALL download :
    @RequestMapping(value="/stopAllDownload", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void stopAllCurrentDownload() {
        IDM.stopAllDownload();
    }

    @RequestMapping(value="/pauseAllDownload", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void pauseAllCurrentDownload() {
        IDM.pauseAllDownload();
    }

    @RequestMapping(value="/restartAllDownload", method = RequestMethod.GET, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void restartAllCurrentDownload() {
        IDM.restartAllDownload();
    }

    // Action on id identified download :
    @RequestMapping(value="/stopDownload", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void stopCurrentDownload(@RequestBody int id) {
        IDM.stopDownload(id);
    }

    @RequestMapping(value="/pauseDownload", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void pauseCurrentDownload(@RequestBody int id) {
        IDM.pauseDownload(id);
    }

    @RequestMapping(value="/restartDownload", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void restartCurrentDownload(@RequestBody int id) {
        IDM.restartDownload(id);
    }

    @RequestMapping(value="/toogleDownload", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void toggleCurrentDownload(@RequestBody int id) {
        IDM.toogleDownload(id);
    }

    @RequestMapping(value="/queue/add", method = RequestMethod.POST, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void addItemToQueue(@RequestBody int id) {
        IDM.addItemToQueue(id);
    }

    @RequestMapping(value="/queue/{id:[\\d]+}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void removeItemFromQueue(@PathVariable int id) {
        IDM.removeItemFromQueue(id, false);
    }

    @RequestMapping(value="/queue/{id:[\\d]+}/andstop", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void removeItemFromQueueAndStopped(@PathVariable int id) {
        IDM.removeItemFromQueue(id, true);

    }


    @RequestMapping(value="/queue", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void emptyQueue() {
        IDM.getWaitingQueue().clear();
    }

}
