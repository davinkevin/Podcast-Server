/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../../../decorators';
import AppVideogular from '../../../common/component/videogular/videogular';
import Truncate from '../../../common/modules/truncate';
import PlaylistService from '../../../common/service/playlistService';
import template from './player-inline.html!text';
import './player-inline.css!';

@Module({
    name : 'ps.common.component.players-inline',
    modules : [ AppVideogular, Truncate, PlaylistService ]
})
@Component({
    selector : 'player-inline',
    as : 'pic',
    replace : true,
    bindings : { podcast : '='},
    template : template
})
export default class PlayerInlineComponent {

    isReading = true;
    hasToBeShown = false;
    state = null;
    API = null;
    currentVideo = {};
    playlist = [];
    config = {
        autoPlay : true,
        sources: [],
        plugins: { controls: { autoHide: null, autoHideTime: 2000}, poster: ''}
    };

    constructor(playlistService, $timeout, deviceDetectorService, $scope) {
        "ngInject";
        this.playlistService = playlistService;
        this.$timeout = $timeout;
        this.deviceDetectorService = deviceDetectorService;
        this.$scope = $scope;
    }

    $onInit() {
        this.config.plugins.controls.autoHide = !this.deviceDetectorService.isTouchedDevice();

        this.$scope.$watchCollection(
            () => this.playlistService.playlist().map(i => i.id), () => this.updateOnPlaylistChange()
        );

        this.reloadPlaylist();
    }

    updateOnPlaylistChange() {
        this.reloadPlaylist();
        this.hasToBeShown = this.playlist.length > 0;

        if (!this.hasToBeShown && this.API && this.API.currentState === 'play') {
            return this.API.stop();
        }

        if (this.hasToStartPlaying()) {
            this.setMedia(0);
        }
    }

    hasToStartPlaying() {
        return this.hasToBeShown && this.API && (this.API.currentState !== 'play' || this.getIndexOfVideoInPlaylist(this.currentVideo) === -1);
    }

    onPlayerReady(API) {
        this.API = API;

        if (this.API.currentState == 'play' || this.isCompleted)
            this.API.play();

        this.isCompleted = false;
        if (this.config.autoPlay) {
            this.$timeout(() => {
                this.setMedia(0);
            }, 1000);
        }
    }

    onCompleteVideo() {
        this.next();
    }

    reloadPlaylist() {
        this.playlist = this.playlistService.playlist();
    }

    setMedia(index) {
        this.currentVideo = this.playlist[index];

        if (this.currentVideo !== null && this.currentVideo !== undefined) {
            this.API.stop();
            this.config.sources = [{src : this.currentVideo.proxyURL, type : this.currentVideo.mimeType }];
            this.config.plugins.poster = this.currentVideo.cover.url;
            this.API.play();
        }
    }

    playPause() {
        this.API.playPause();
    }

    next() {
        let indexOfVideo = this.getIndexOfVideoInPlaylist(this.currentVideo);

        if (indexOfVideo+1 === this.playlist.length) {
            return;
        }

        this.setMedia(indexOfVideo+1);
    }

    previous() {
        let indexOfVideo = this.getIndexOfVideoInPlaylist(this.currentVideo);

        if (indexOfVideo === 0) {
            this.setMedia(this.playlist.length-1);
            return;
        }

        this.setMedia(indexOfVideo-1);
    }

    clearPlaylist() {
        this.playlistService.removeAll();
    }

    getIndexOfVideoInPlaylist(item) {
        return this.playlist.indexOf(item);
    }

    removeFromPlaylist(item) {
        this.playlistService.remove(item);
    }

    play(item) {
        let indexToRead = this.getIndexOfVideoInPlaylist(item);

        if (indexToRead === this.getIndexOfVideoInPlaylist(this.currentVideo)){
            this.playPause();
            return;
        }

        this.setMedia(indexToRead);
    }

    isCurrentlyPlaying(item) {
        return item.id === this.currentVideo.id;
    }
}