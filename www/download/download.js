/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import _ from 'lodash';
import {RouteConfig, View, HotKeys, Module} from '../decorators';
import AngularNotification from '../common/modules/angularNotification';
import AppRouteConfig from '../config/route.config';
import DownloadManager from '../common/service/data/downloadManager';
import template from './download.html!text';

@Module({
    name : 'ps.download',
    modules : [
        AngularNotification,
        AppRouteConfig,
        DownloadManager
    ]
})
@RouteConfig({
    path : '/download',
    as : 'dc'
})
@HotKeys({})
@View({
    template : template
})
export default class DownloadCtrl {

    constructor($scope, DonwloadManager, $notification) {
        "ngInject";
        this.DonwloadManager = DonwloadManager;
        this.$notification = $notification;
        this.items =[];
        this.waitingitems = [];
        this.numberOfSimDl = 0;

        this.DonwloadManager.getNumberOfSimDl().then((value) => {
            this.numberOfSimDl = parseInt(value);
        });

        /** Websocket Connection */
        this.DonwloadManager.ws
            .subscribe("/app/download", (message) => this.onSubscribeDownload(message), $scope)
            .subscribe("/app/waiting", (message) => this.onSubscribeWaiting(message), $scope)
            .subscribe("/topic/download", (message) => this.onDownloadUpdate(message), $scope)
            .subscribe("/topic/waiting", (message) => this.onWaitingUpdate(message), $scope);
    }

    onSubscribeDownload(message) {
        this.items = JSON.parse(message.body);
    }
    onSubscribeWaiting(message) {
        this.waitingitems = JSON.parse(message.body);
    }
    onDownloadUpdate(message) {
        let item = JSON.parse(message.body);
        let elemToUpdate = _.find(this.items, { 'id': item.id });
        switch (item.status) {
            case 'STARTED' :
            case 'PAUSED' :
                if (elemToUpdate)
                    _.assign(elemToUpdate, item);
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
        if (elemToUpdate) {
            _.remove(this.items, function (item) {
                return item.id === elemToUpdate.id;
            });
        }
    }

    onWaitingUpdate(message) {
        let remoteWaitingItems = JSON.parse(message.body);
        _.updateinplace(this.waitingitems, remoteWaitingItems, (inArray, elem) => _.findIndex(inArray, { 'id': elem.id }), true);
    }

    getTypeFromStatus(item) {
        if (item.status === "PAUSED")
            return "warning";
        return "info";
    }
    updateNumberOfSimDl(number) {
        this.DonwloadManager.updateNumberOfSimDl(number);
    }

    /** Spécifique aux éléments de la liste : **/
    download(item){
        this.DonwloadManager.download(item);
    }
    stopDownload(item){
        this.DonwloadManager.ws.stop(item);
    }
    toggleDownload(item){
        this.DonwloadManager.ws.toggle(item);
    }

    /** Global **/
    stopAllDownload(){
        this.DonwloadManager.stopAllDownload();
    }
    pauseAllDownload(){
        this.DonwloadManager.pauseAllDownload();
    }
    restartAllCurrentDownload(){
        this.DonwloadManager.restartAllCurrentDownload();
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
