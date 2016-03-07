import {Module, Service} from '../../../decorators';
import AngularStompDKConfig from '../../../config/ngstomp';

class wsDownloadManager {
    /*@ngNoInject*/
    constructor(urlBase, ngstomp) {
        this.WS_DOWNLOAD_BASE = urlBase;
        this.ngstomp = ngstomp;
    }

    connect() { return this.ngstomp.connect();}
    subscribe(url, callback, scope) {
        this.ngstomp.subscribe(url, callback, {}, scope); return this;
    }
    unsubscribe(url) {
        this.ngstomp.unsubscribe(url); return this;
    }
    toggle(item) { this.ngstomp.send(this.WS_DOWNLOAD_BASE + '/toogle', item); return this;}
    start(item) { this.ngstomp.send(this.WS_DOWNLOAD_BASE + '/start', item); return this;}
    pause(item) { this.ngstomp.send(this.WS_DOWNLOAD_BASE + '/pause', item); return this;}
    stop(item) { this.ngstomp.send(this.WS_DOWNLOAD_BASE + '/stop', item); return this;}
}


@Module({
    name : 'ps.common.service.data.downloadManager',
    modules : [ AngularStompDKConfig ]
})
@Service('DonwloadManager')
export default class DownloadManager {

    WS_DOWNLOAD_BASE = '/app/download';

    constructor(ngstomp, $http) {
        "ngInject";
        this.$http = $http;
        this.ws = new wsDownloadManager(this.WS_DOWNLOAD_BASE, ngstomp);
    }

    download(item) {
        return this.$http.get(`/api/item/${item.id}/addtoqueue`);
    }
    stopAllDownload () {
        return this.$http.get(`/api/task/downloadManager/stopAllDownload`);
    }
    pauseAllDownload () {
        return this.$http.get(`/api/task/downloadManager/pauseAllDownload`);
    }
    restartAllDownload() {
        return this.$http.get(`/api/task/downloadManager/restartAllDownload`);
    }
    removeFromQueue (item) {
        return this.$http.delete(`/api/task/downloadManager/queue/${item.id}`);
    }
    updateNumberOfSimDl(number) {
        return this.$http.post(`/api/task/downloadManager/limit`, number);
    }
    dontDonwload (item) {
        return this.$http.delete(`/api/task/downloadManager/queue/${item.id}/andstop`);
    }
    getNumberOfSimDl() {
        return this.$http.get(`/api/task/downloadManager/limit`).then(r => r.data);
    }
    moveInWaitingList(item, position) {
        return this.$http.post(`/api/task/downloadManager/move`, {id : item.id, position });
    }
}