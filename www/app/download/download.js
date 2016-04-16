/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../decorators';
import AngularNotification from '../common/modules/angularNotification';
import AppRouteConfig from '../config/route';
import DownloadManager from '../common/service/data/downloadManager';
import template from './download.html!text';

@Module({
    name : 'ps.download',
    modules : [AngularNotification, AppRouteConfig, DownloadManager]
})
@Component({
    selector : 'download',
    as : 'dc',
    template : template,

    path : '/download'
})
export default class DownloadCtrl {

    waitingitems = [];
    numberOfSimDl = 0;
    items = [];

    constructor($scope, DonwloadManager, $notification) {
        "ngInject";
        this.$scope = $scope;
        this.DonwloadManager = DonwloadManager;
        this.$notification = $notification;
    }

    $onInit() {
        this.DonwloadManager.getNumberOfSimDl().then(v => { this.numberOfSimDl = parseInt(v); });

        /** Websocket Connection */
        this.DonwloadManager
            .ngstomp
                .subscribeTo('/app/download').withBodyInJson().bindTo(this.$scope)
                .callback(m => this.items = m.body)
            .and()
                .subscribeTo('/app/waiting').withBodyInJson().bindTo(this.$scope)
                .callback(m => this.waitingitems = m.body)
            .and()
                .subscribeTo('/topic/download').withBodyInJson().bindTo(this.$scope)
                .callback(m => this.onDownloadUpdate(m.body))
            .and()
                .subscribeTo('/topic/waiting').withBodyInJson().bindTo(this.$scope)
                .callback(m => this.waitingitems = m.body)
            .connect();
    }

    onDownloadUpdate(item) {
        let elemToUpdate = this.items.find(i => i.id === item.id);
        switch (item.status) {
            case 'STARTED' :
            case 'PAUSED' :
                if (elemToUpdate)
                    Object.assign(elemToUpdate, item);
                else
                    this.items.push(item);
                break;
            case 'FINISH' :
                this.$notification('Téléchargement terminé', {
                    body: item.title,
                    icon: item.cover.url,
                    delay: 5000
                });
                this.onStoppedFromWS(elemToUpdate);
                break;
            case 'STOPPED' :
                this.onStoppedFromWS(elemToUpdate);
                break;
        }
    }

    onStoppedFromWS(elemToUpdate) {
        if (elemToUpdate) { this.items = this.items.filter(i => i.id !== elemToUpdate.id); }
    }

    getTypeFromStatus(item) {
        return (item.status === "PAUSED") ? "warning" : "info";
    }
    updateNumberOfSimDl(number) {
        this.DonwloadManager.updateNumberOfSimDl(number);
    }

    /** Spécifique aux éléments de la liste : **/
    download(item){
        this.DonwloadManager.download(item);
    }
    stopDownload(item){
        this.DonwloadManager.stop(item);
    }
    toggleDownload(item){
        this.DonwloadManager.toggle(item);
    }

    /** Global **/
    stopAllDownload(){
        this.DonwloadManager.stopAllDownload();
    }
    pauseAllDownload(){
        this.DonwloadManager.pauseAllDownload();
    }
    restartAllDownload(){
        this.DonwloadManager.restartAllDownload();
    }
    removeFromQueue(item){
        this.DonwloadManager.removeFromQueue(item);
    }
    dontDonwload(item){
        this.DonwloadManager.dontDonwload(item);
    }
    moveInWaitingList(item, position){
        this.DonwloadManager.moveInWaitingList(item, position);
    }
}
