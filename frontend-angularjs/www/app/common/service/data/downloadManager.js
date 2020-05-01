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

        const sse = new EventSource("/api/v1/sse");

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

    download(item) {
        return this.$http.post(`/api/v1/podcasts/${item.podcast.id}/items/${item.id}/download`);
    }

    downloading() {
        return this.$http.get(`/api/v1/downloads/downloading`).then(r => r.data.items);
    }

    stopAllDownload () {
        return this.$http.post(`/api/v1/downloads/stop`);
    }
    pauseAllDownload () {
        return this.$http.post(`/api/v1/downloads/pause`);
    }
    restartAllDownload() {
        return this.$http.post(`/api/v1/downloads/restart`);
    }

    updateNumberOfSimDl(number) {
        return this.$http.post(`/api/v1/downloads/limit`, number);
    }
    getNumberOfSimDl() {
        return this.$http.get(`/api/v1/downloads/limit`).then(r => r.data);
    }

    queue() {
        return this.$http.get(`/api/v1/downloads/queue`).then(r => r.data.items);
    }
    moveInWaitingList(item, position) {
        return this.$http.post(`/api/v1/downloads/queue`, {id : item.id, position });
    }
    dontDonwload (item) {
        return this.$http.delete(`/api/v1/downloads/queue/${item.id}?stop=true`);
    }
    removeFromQueue (item) {
        return this.$http.delete(`/api/v1/downloads/queue/${item.id}`);
    }


    toggle(item) {
      return this.$http.post(`/api/v1/downloads/${item.id}/toggle`);
    }
    stop(item) {
      return this.$http.post(`/api/v1/downloads/${item.id}/stop`);
    }
}
