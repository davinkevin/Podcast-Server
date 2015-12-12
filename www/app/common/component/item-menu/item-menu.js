/**
    * Created by kevin on 05/12/2015 for Podcast Server
    */

import _ from 'lodash';
import {Component, View, Module} from '../../../decorators';
import DownloadManager from '../../service/data/downloadManager';
import playlistService from '../../service/playlistService';
import Copy from '../copy/copy';
import template from './item-menu.html!text';
import './item-menu.css!';

@Module({
    name : 'ps.common.component.item-menu',
    modules : [ DownloadManager, playlistService, Copy ]
})
@Component({
    selector : 'item-menu',
    as : 'imc',
    bindToController : {
        item : '=',
        localRead : '=',
        onLineRead : '=',
        downloadControl : '=',
        readInPlayer : '=',
        playlistControl : '=',
        deleteItem : '=',
        onDeleteItem : '&',
        resetItem : '='
    }
})
@View({
    template : template
})
export default class ItemMenuComponent {
    constructor(DonwloadManager, playlistService) {
        "ngInject";
        this.DownloadManager = DonwloadManager;
        this.playlistService = playlistService;
    }

    remove(item) {
        return item.remove()
            .then(() => this.playlistService.remove(item))
            .then(() => this.onDeleteItem());
    }

    reset(item) {
        return item.reset()
            .then((itemReseted) => _.assign(item, itemReseted))
            .then((itemInList) => this.playlistService.remove(itemInList));
    }

    stopDownload(item) {
        this.DownloadManager.ws.stop(item);
    }

    toggleDownload(item){
        return this.DownloadManager.ws.toggle(item);
    }

    addOrRemove(item) {
        return this.playlistService.addOrRemove(item);
    }

    isInPlaylist(item) {
        return this.playlistService.contains(item);
    }
}

