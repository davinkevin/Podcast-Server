/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../../decorators';
import {TitleService} from '../../common/service/title.service';
import WatchListChooser from '../../common/component/watchlist-chooser/watchlist-chooser';
import DownloadManager from '../../common/service/data/downloadManager';
import PlaylistService from '../../common/service/playlistService';
import ItemService from '../../common/service/data/itemService';
import HtmlFilters from '../../common/filter/html2plainText';
import template from './item-details.html!text';

@Module({
    name : 'ps.item.details',
    modules : [ DownloadManager, HtmlFilters, PlaylistService, ItemService, WatchListChooser, TitleService ]
})
@Component({
    selector : 'item-detail',
    as : 'idc',
    template : template,

    path : '/podcasts/:podcastId/items/:itemId',
    resolve : {
        item : (itemService, $route) => { "ngInject"; return itemService.findById($route.current.params.podcastId, $route.current.params.itemId);},
        podcast : (podcastService, $route) => { "ngInject"; return podcastService.findById($route.current.params.podcastId);}
    }
    
})
export default class ItemDetailCtrl {

    constructor($scope, DonwloadManager, $location, playlistService, itemService, $uibModal, TitleService){
        "ngInject";
        this.$uibModal = $uibModal;
        this.itemService = itemService;
        this.$location = $location;
        this.playlistService = playlistService;
        this.DonwloadManager = DonwloadManager;
        this.$scope = $scope;
        this.TitleService = TitleService;
    }

    $onInit() {
        this.TitleService.title = this.item.title;

        this.item.podcast = this.podcast;

        this.subscription = this.DonwloadManager
            .download$
            .filter(item => item.id === this.item.id)
            .subscribe(item => this.$scope.$evalAsync(() => Object.assign(this.item, item)));
    }
    
    $onDestroy() {
        this.subscription.dispose();
    }

    download() {
        return this.itemService.download(this.item);
    }

    stopDownload(item) {
        this.DonwloadManager.stop(item);
    }

    toggleDownload(item) {
        this.DonwloadManager.toggle(item);
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