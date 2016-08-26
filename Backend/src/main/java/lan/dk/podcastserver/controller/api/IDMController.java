package lan.dk.podcastserver.controller.api;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.manager.ItemDownloadManager;
import lan.dk.podcastserver.utils.form.MovingItemInQueueForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Created by kevin on 26/12/2013.
 */
@Slf4j
@RestController
@RequestMapping("/api/task/downloadManager")
@RequiredArgsConstructor
public class IDMController {

    final ItemDownloadManager IDM;

    @RequestMapping(value="/queue", method = RequestMethod.GET)
    public Queue<Item> getDownloadList () {
        return IDM.getWaitingQueue();
    }

    @RequestMapping(value="/downloading", method = RequestMethod.GET)
    public Set<Item> getDownloadingList () {
        return IDM.getDownloadingQueue().keySet();
    }

    @RequestMapping(value="/downloading/{id}", method = RequestMethod.GET)
    public Item getDownloadingList (@PathVariable UUID id) {
        return IDM.getItemInDownloadingQueue(id);
    }

    @RequestMapping(value="/current", method = RequestMethod.GET)
    public int getNumberOfCurrentDownload () {
        return IDM.getNumberOfCurrentDownload();
    }

    @RequestMapping(value="/limit", method = RequestMethod.GET)
    public int getLimitParallelDownload () {
        return IDM.getLimitParallelDownload();
    }

    @RequestMapping(value="/limit", method = RequestMethod.POST)
    public void setLimitParallelDownload (@RequestBody int setLimitParallelDownload) {
        IDM.setLimitParallelDownload(setLimitParallelDownload);
    }

    @RequestMapping(value="/launch", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void launchDownload() {
        IDM.launchDownload();
    }

    // Action on ALL download :
    @RequestMapping(value="/stopAllDownload", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void stopAllCurrentDownload() {
        IDM.stopAllDownload();
    }

    @RequestMapping(value="/pauseAllDownload", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void pauseAllCurrentDownload() {
        IDM.pauseAllDownload();
    }

    @RequestMapping(value="/restartAllDownload", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void restartAllCurrentDownload() {
        IDM.restartAllDownload();
    }

    // Action on id identified download :
    @RequestMapping(value="/stopDownload", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void stopCurrentDownload(@RequestBody UUID id) {
        IDM.stopDownload(id);
    }

    @RequestMapping(value="/pauseDownload", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void pauseCurrentDownload(@RequestBody UUID id) {
        IDM.pauseDownload(id);
    }

    @RequestMapping(value="/restartDownload", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void restartCurrentDownload(@RequestBody UUID id) {
        IDM.restartDownload(id);
    }

    @RequestMapping(value="/toogleDownload", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void toggleCurrentDownload(@RequestBody UUID id) {
        IDM.toogleDownload(id);
    }

    @RequestMapping(value="/queue/add", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void addItemToQueue(@RequestBody UUID id) {
        IDM.addItemToQueue(id);
    }

    @RequestMapping(value="/queue/{id}", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void removeItemFromQueue(@PathVariable UUID id) {
        IDM.removeItemFromQueue(id, false);
    }

    @RequestMapping(value="/queue/{id}/andstop", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void removeItemFromQueueAndStopped(@PathVariable UUID id) {
        IDM.removeItemFromQueue(id, true);
    }

    @RequestMapping(value="/queue", method = RequestMethod.DELETE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void emptyQueue() {
        IDM.getWaitingQueue().clear();
    }

    @RequestMapping(value="/move", method = RequestMethod.POST)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void moveItemInQueue(@RequestBody MovingItemInQueueForm movingItemInQueueForm) {
        IDM.moveItemInQueue(movingItemInQueueForm.getId(), movingItemInQueueForm.getPosition());
    }
}
