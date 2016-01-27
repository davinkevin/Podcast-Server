/**
 * Created by kevin on 25/10/2015 for Podcast Server
 */
import {RouteConfig, View, HotKeys, Module} from '../decorators';
import Videogular from '../common/modules/videogular/videogular';
import AppRouteConfig from '../config/route';
import DeviceDetectionService from '../common/service/device-detection';
import PlaylistService from '../common/service/playlistService';
import template from './player.html!text';
import './player.css!';

@Module({
    name : 'ps.player',
    modules : [ AppRouteConfig, Videogular, DeviceDetectionService, PlaylistService ]
})
@RouteConfig({
    path : '/player',
    as : 'pc'
})
@HotKeys({})
@View({
    template : template
})
export default class PlayerController {
    constructor(playlistService, $timeout, deviceDetectorService) {
        "ngInject";
        this.playlistService = playlistService;
        this.$timeout = $timeout;

        this.playlist = [];
        this.state = null;
        this.API = null;
        this.currentVideo = {};
        this.config = {
            autoPlay : true,
            sources: [],
            plugins: {
                controls: {
                    autoHide : !deviceDetectorService.isTouchedDevice(),
                    autoHideTime: 2000
                },
                poster: ''
            }
        };
        this.reloadPlaylist();
    }

    onPlayerReady(API) {
        this.API = API;

        if (this.API.currentState == 'play' || this.isCompleted)
            this.API.play();

        this.isCompleted = false;
        this.setVideo(0);
    }

    onCompleteVideo() {
        var indexOfVideo = this.getIndexOfVideoInPlaylist(this.currentVideo);
        this.isCompleted = true;

        if (indexOfVideo+1 === this.playlist.length) {
            this.currentVideo = this.playlist[0];
            return;
        }

        this.setVideo(indexOfVideo+1);
    }

    reloadPlaylist() {
        this.playlist = this.playlistService.playlist();
    }

    setVideo(index) {
        this.currentVideo = this.playlist[index];

        if (this.currentVideo !== null && this.currentVideo !== undefined) {
            this.API.stop();
            this.config.sources = [{src : this.currentVideo.proxyURL, type : this.currentVideo.mimeType }];
            this.config.plugins.poster = this.currentVideo.cover.url;
        }
    }

    remove(item) {
        this.playlistService.remove(item);
        this.reloadPlaylist();
        if (this.config.sources.length > 0 && this.config.sources[0].src === item.proxyURL) {
            this.setVideo(0);
        }
    }

    removeAll() {
        this.playlistService.removeAll();
        this.reloadPlaylist();
    }

    getIndexOfVideoInPlaylist(item) {
        return this.playlist.indexOf(item);
    }
}