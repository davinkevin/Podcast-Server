import {Module, Service} from '../../../decorators';
import AngularStompDKConfig from '../../../config/ngstomp';
import Rx from 'rx';

@Module({
    name : 'ps.common.service.data.downloadManager',
    modules : [ AngularStompDKConfig ]
})
@Service('DonwloadManager')
export default class DownloadManager {

    WS_DOWNLOAD_BASE = '/app/download';
    download$ = new Rx.ReplaySubject(0);
    waiting$ = new Rx.ReplaySubject(0);
    updating$ = new Rx.BehaviorSubject(null);
    
    constructor(ngstomp, $http) {
        "ngInject";
        this.$http = $http;
        this.ngstomp = ngstomp;

        this.ngstomp.subscribeTo('/topic/download').withBodyInJson().withDigest(false)
                .callback(m => this.download$.onNext(m.body))
            .and()
                .subscribeTo('/topic/waiting').withBodyInJson().withDigest(false)
                .callback(m => this.waiting$.onNext(m.body))
            .and()
                .subscribeTo('/topic/updating').withBodyInJson().withDigest(false)
                .callback(m => this.updating$.onNext(m.body))
            .connect();
    }

    downloading() {
        return this.$http.get(`/api/task/downloadManager/downloading`).then(r => r.data);
    }

    queue() {
        return this.$http.get(`/api/task/downloadManager/queue`).then(r => r.data);
    }

    download(item) {
        return this.$http.get(`/api/items/${item.id}/addtoqueue`);
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
    toggle(item) {
        this.ngstomp.send(`${this.WS_DOWNLOAD_BASE}/toogle`, item);
    }
    stop(item) {
        this.ngstomp.send(`${this.WS_DOWNLOAD_BASE}/stop`, item);
    }
}