/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import _ from 'lodash';
import {RouteConfig, View, Module} from '../../decorators';
import WatchListChooser from '../../common/component/watchlist-chooser/watchlist-chooser';
import DownloadManager from '../../common/service/data/downloadManager';
import PlaylistService from '../../common/service/playlistService';
import ItemService from '../../common/service/data/itemService';
import HtmlFilters from '../../common/filter/html2plainText';
import template from './item-details.html!text';

@Module({
    name : 'ps.item.details',
    modules : [ DownloadManager, HtmlFilters, PlaylistService, ItemService, WatchListChooser ]
})
@RouteConfig({
    path : '/podcasts/:podcastId/item/:itemId',
    as : 'idc',
    resolve : {
        item : (itemService, $route) => { "ngInject"; return itemService.findById($route.current.params.podcastId, $route.current.params.itemId);},
        podcast : (podcastService, $route) => { "ngInject"; return podcastService.findById($route.current.params.podcastId);}
    }
})
@View({
    template : template
})
export default class ItemDetailCtrl {

    constructor($scope, DonwloadManager, $location, playlistService, podcast, item, itemService, $uibModal){
        "ngInject";
        this.$uibModal = $uibModal;
        this.itemService = itemService;
        this.item = item;
        this.$location = $location;
        this.item.podcast = podcast;
        this.playlistService = playlistService;
        this.DonwloadManager = DonwloadManager;

        //** WebSocket Inscription **//
        let webSockedUrl = "/topic/podcast/".concat(this.item.podcast.id);

        this.DonwloadManager
            .ws
            .subscribe(webSockedUrl, (message) => {
                let itemFromWS = JSON.parse(message.body);
                if (itemFromWS.id == this.item.id) {
                    _.assign(this.item, itemFromWS);
                }
            }, $scope);

    }

    stopDownload(item) {
        this.DonwloadManager.ws.stop(item);
    }

    toggleDownload(item) {
        this.DonwloadManager.ws.toggle(item);
    }

    redirect() {
        this.$location.path('/podcasts/'.concat(this.item.podcast.id));
    }

    toggleInPlaylist() {
        this.playlistService.addOrRemove(this.item);
    }

    isInPlaylist() {
        return this.playlistService.contains(this.item);
    }

    play() {
        return this.itemService.play(this.item);
    }

    isVideo() {
        return ItemService.isVideo(this.item);
    }

    addToWatchList() {
        return this.$uibModal.open(WatchListChooser.$UibModalConf.withResolve({
            item : () => this.item,
            watchListsOfItem : WatchListService => {"ngInject"; return WatchListService.findAllWithItem(this.item);}
        }));
    }

}