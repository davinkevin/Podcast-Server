class DownloadManager {

    constructor(Restangular, ngstomp) {
        this.Restangular = Restangular;
        this.baseTask = this.Restangular.one("task");
        this.baseDownloadManager = this.baseTask.one('downloadManager');
        this.WS_DOWNLOAD_BASE = '/app/download';

        this.ws = {
            connect : ngstomp.connect,
            subscribe : (url, callback, scope) => ngstomp.subscribe(url, callback, scope),
            unsubscribe : (url) => ngstomp.unsubscribe(url),
            toggle : (item) => { ngstomp.send(this.WS_DOWNLOAD_BASE + '/toogle', item); },
            start : (item) => { ngstomp.send(this.WS_DOWNLOAD_BASE + '/start', item); },
            pause : (item) => { ngstomp.send(this.WS_DOWNLOAD_BASE + '/pause', item); },
            stop : (item) => { ngstomp.send(this.WS_DOWNLOAD_BASE + '/stop', item); }
        };
    }

    download(item) {
        return this.Restangular.one("item").customGET(item.id + "/addtoqueue");
    }
    stopDownload (item) {
        return this.baseDownloadManager.customPOST(item.id, "stopDownload");
    }
    toggleDownload (item) {
        return this.baseDownloadManager.customPOST(item.id, "toogleDownload");
    }
    stopAllDownload () {
        return this.baseDownloadManager.customGET("stopAllDownload");
    }
    pauseAllDownload () {
        return this.baseDownloadManager.customGET("pauseAllDownload");
    }
    restartAllCurrentDownload () {
        return this.baseDownloadManager.customGET("restartAllCurrentDownload");
    }
    removeFromQueue (item) {
        return this.baseDownloadManager.customDELETE("queue/" + item.id);
    }
    updateNumberOfSimDl (number) {
        return this.baseDownloadManager.customPOST(number, "limit");
    }
    dontDonwload (item) {
        return this.baseDownloadManager.customDELETE("queue/" + item.id + "/andstop");
    }
    getDownloading () {
        return this.baseTask.all("downloadManager/downloading").getList();
    }
    getNumberOfSimDl () {
        return this.baseDownloadManager.one("limit").get();
    }
    moveInWaitingList  (item, position) {
        this.baseDownloadManager.customPOST({id : item.id, position : position } , 'move');
    }

}

angular.module('ps.dataService.donwloadManager', [ 'restangular', 'AngularStompDK']) .service('DonwloadManager', DownloadManager);