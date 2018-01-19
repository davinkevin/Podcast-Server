/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../decorators';
import {TitleService} from '../common/service/title.service';
import AngularNotification from '../common/modules/angularNotification';
import AppRouteConfig from '../config/route';
import DownloadManager from '../common/service/data/downloadManager';
import template from './download.html!text';

@Module({
    name : 'ps.download',
    modules : [AngularNotification, AppRouteConfig, DownloadManager, TitleService]
})
@Component({
    selector : 'download',
    as : 'dc',
    template : template,

    path : '/download',
    resolve : {
        waitingItems : DonwloadManager => {"ngInject"; return DonwloadManager.queue();},
        items : DonwloadManager => {"ngInject"; return DonwloadManager.downloading();}
    }
})
export default class DownloadCtrl {

    //waitingItems = [];
    numberOfSimDl = 0;
    //items = [];

    constructor($scope, DonwloadManager, $notification, TitleService) {
        "ngInject";
        this.$scope = $scope;
        this.DonwloadManager = DonwloadManager;
        this.$notification = $notification;
        this.TitleService = TitleService;
    }

    $onInit() {
        this.TitleService.title = 'Download';

        this.DonwloadManager.getNumberOfSimDl().then(v => { this.numberOfSimDl = parseInt(v); });

        this.DonwloadManager.waiting$.onNext(this.waitingItems);

        this.downloadSub = this.DonwloadManager
            .download$
            .subscribe(m => this.$scope.$evalAsync(() => this.onDownloadUpdate(m)));

        this.waitingSub = this.DonwloadManager
            .waiting$
            .filter(v => v)
            .subscribe(m => this.$scope.$evalAsync(() => this.waitingItems = m));
    }

    $onDestroy() {
        this.downloadSub.dispose();
        this.waitingSub.dispose();
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
            case 'FAILED' :
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
