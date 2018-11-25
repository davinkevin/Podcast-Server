import {Module, Service} from '../../../decorators';
import Rx from 'rx';

@Module({
    name : 'ps.common.service.data.downloadManager'
})
@Service('DonwloadManager')
export default class DownloadManager {

    WS_DOWNLOAD_BASE = '/app/download';
    download$ = new Rx.ReplaySubject(0);
    waiting$ = new Rx.ReplaySubject(0);
    updating$ = new Rx.BehaviorSubject(null);
    
    constructor($http) {
        "ngInject";
        this.$http = $http;

        const sse = new EventSource("/api/sse");

        sse.addEventListener("updating", (m) =>  {
          this.updating$.onNext(JSON.parse(m.data));
        });
        sse.addEventListener("waiting", (m) =>  {
          this.waiting$.onNext(JSON.parse(m.data));
        });
        sse.addEventListener("downloading", (m) =>  {
          this.download$.onNext(JSON.parse(m.data));
        });
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
      return this.$http.post(`/api/task/downloadManager/toogleDownload/${item.id}`);
    }
    stop(item) {
      return this.$http.post(`/api/task/downloadManager/stopDownload/${item.id}`);
    }
}