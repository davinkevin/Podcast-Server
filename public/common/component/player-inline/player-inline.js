
class PlayerInlineDirective {
    constructor() {
        this.replace = true;
        this.restrict = 'E';
        this.scope = true;
        this.templateUrl = 'common/component/player-inline/player-inline.html';
        this.controller = 'PlayerInlineController';
        this.controllerAs = 'pic';
    }
}

class PlayerInlineController {
    constructor(playlistService, $timeout, deviceDetectorService, $scope) {
        this.isReading = true;
        this.playlistService = playlistService;
        this.$timeout = $timeout;
        this.hasToBeShown = false;
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

        $scope.$watch(
            () => this.playlistService.playlist().length,
            (newVal) => this.updateOnPlaylistChange(newVal)
        );
        this.reloadPlaylist();
    }

    updateOnPlaylistChange(newVal) {
        this.hasToBeShown = newVal > 0;
        this.reloadPlaylist();

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
        _.updateinplace(
            this.playlist,
            this.playlistService.playlist(),
            function(inArray, elem) {
                return _.findIndex(inArray, { 'id': elem.id });
            }
        );
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
            this.setMedia(0);
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

angular.module('ps.common.component.players-inline', [
    'ngSanitize',
    'com.2fdevs.videogular',
    'com.2fdevs.videogular.plugins.poster',
    'com.2fdevs.videogular.plugins.controls',
    'com.2fdevs.videogular.plugins.overlayplay',
    'com.2fdevs.videogular.plugins.buffering',
    'ps.common.service.playlist',
    'truncate'
])
    .directive('playerInline', () => new PlayerInlineDirective())
    .controller('PlayerInlineController', PlayerInlineController);
