/**
 * Created by kevin on 25/10/2015 for Podcast Server
 */
import {Component, Module} from '../decorators';
import {TitleService} from '../common/service/title.service';
import VideogularModule from '../common/component/videogular/videogular';
import AppRouteConfig from '../config/route';
import DeviceDetectionService from '../common/service/device-detection';
import PlaylistService from '../common/service/playlistService';
import WatchListService from '../common/service/data/watchlistService';
import template from './player.html!text';
import './player.css!';

@Module({
    name : 'ps.player',
    modules : [ AppRouteConfig, VideogularModule, DeviceDetectionService, PlaylistService, WatchListService, TitleService ]
})
@Component({
    selector : 'player',
    as : 'pc',
    template : template,
    
    path : '/player',
    resolve : { watchLists : WatchListService => { "ngInject"; return WatchListService.findAll();}}
})
export default class PlayerController {

    state = null;
    API = null;

    currentVideo = {};
    currentWatchList = null;

    playlist = null;

    constructor(VideogularService, WatchListService, TitleService) {
        "ngInject";
        this.watchListService = WatchListService;
        this.VideogularService = VideogularService;
        this.TitleService = TitleService;
    }

    $onInit() {
        this.TitleService.title = 'Player';
        this.config = this.VideogularService.builder().build();
    }

    play() {
        if (this.hasAWatchListWithItems())
            return this.setVideo(0);
    }

    onPlayerReady(API) {
        this.API = API;

        if (this.API.currentState == 'play' || this.isCompleted)
            this.API.play();

        this.isCompleted = false;
        this.play();
    }

    onCompleteVideo() {
        var indexOfVideo = this.getIndexOfVideoInPlaylist(this.currentVideo);
        this.isCompleted = true;

        if (indexOfVideo+1 === this.currentWatchList.items.length) {
            [this.currentVideo] = this.currentWatchList.items;
            return;
        }

        this.setVideo(indexOfVideo+1);
    }

    setVideo(index) {
        this.currentVideo = this.currentWatchList.items[index];

        if (this.currentVideo !== null && this.currentVideo !== undefined) {
            this.API.stop();
            this.config.sources = [{src : this.currentVideo.proxyURL, type : this.currentVideo.mimeType }];
            this.config.plugins.poster = this.currentVideo.cover.url;
        }
    }

    remove(item) {
        this.watchListService
            .removeItemFromWatchList(this.currentWatchList, item)
            .then(() => this.currentWatchList.items.splice(this.currentWatchList.items.indexOf(item), 1))
            .then(() => this.isCurrentlyPlaying(item) &&  this.play());
    }

    isCurrentlyPlaying(item) {
        return this.config.sources.length > 0 && this.config.sources[0].src === item.proxyURL;
    }

    removeAll() {
        this.API.clearMedia();
        this.$q
            .all(this.currentWatchList.items.map(i => this.watchListService.removeItemFromWatchList(this.currentWatchList, i)))
            .then(() => this.currentWatchList = this.watchListService.findOne(this.currentWatchList.id))
            .then(() => this.start())
        ;
    }

    getIndexOfVideoInPlaylist(item) {
        return this.currentWatchList.items.indexOf(item);
    }

    loadWatchList() {
        this.watchListService.findAll()
            .then(watchLists => this.watchLists = watchLists);
    }

    selectWatchList(watchList) {
        return this.watchListService
            .findOne(watchList.id)
            .then( w => {
                this.currentWatchList = w;
                this.watchLists = null;
            })
            .then( () => this.play());
    }

    removeWatchList(watchList) {
        return this.watchListService
                .delete(watchList.id)
                .then(() => this.loadWatchList());
    }

    hasAWatchListWithItems() {
        return this.currentWatchList && this.currentWatchList.items.length > 0;
    }
}