angular.module('ps.player', [
    'ngSanitize',
    'ngRoute',
    'ngStorage',
    'device-detection',
    'com.2fdevs.videogular',
    'com.2fdevs.videogular.plugins.poster',
    'com.2fdevs.videogular.plugins.controls',
    'com.2fdevs.videogular.plugins.overlayplay',
    'com.2fdevs.videogular.plugins.buffering'
])
    .config(function($routeProvider) {
        $routeProvider.
            when('/player', {
                templateUrl: 'html/player.html',
                controller: 'PlayerController',
                controllerAs: 'pc'
            });
    })
    .controller('PlayerController', function (playlistService, $timeout, deviceDetectorService) {
        var vm = this;
        
        vm.playlist = [];
        vm.state = null;
        vm.API = null;
        vm.currentVideo = {};

        vm.onPlayerReady = function(API) {
            vm.API = API;

            if (vm.API.currentState == 'play' || vm.isCompleted) 
                vm.API.play();

            vm.isCompleted = false;
            vm.setVideo(0)
        };

        vm.onCompleteVideo = function() {
            var indexOfVideo = getIndexOfVideoInPlaylist(vm.currentVideo);
            vm.isCompleted = true;

            if (indexOfVideo+1 === vm.playlist.length) {
                vm.currentVideo = vm.playlist[0];
                return;
            }

            vm.setVideo(indexOfVideo+1);
        };
        

        vm.config = {
            preload : true,
            sources: [],
            theme: {
                url: "http://www.videogular.com/styles/themes/default/videogular.css"
            },
            plugins: {
                controls: {
                    autoHide : !deviceDetectorService.isTouchedDevice(),
                    autoHideTime: 2000
                },
                poster: ''
            }
        };

        vm.reloadPlaylist = function() {
            _.updateinplace(vm.playlist, playlistService.playlist(), function(inArray, elem) { return _.findIndex(inArray, { 'id': elem.id });});
        };
        
        vm.reloadPlaylist();
        
        vm.setVideo = function(index) {
            vm.currentVideo = vm.playlist[index];

            if (vm.currentVideo !== null && vm.currentVideo !== undefined) {
                vm.API.stop();
                vm.config.sources = [{src : vm.currentVideo.localUrl, type : vm.currentVideo.mimeType }];
                vm.config.plugins.poster = vm.currentVideo.cover.url;
                if (vm.config.preload) {
                    $timeout(function() { vm.API.play(); }, 500);
                }
            }
        };
        
        vm.remove = function(item) {
            playlistService.remove(item);
            vm.reloadPlaylist();
            if (vm.config.sources.length > 0 && vm.config.sources[0].src === item.localUrl) {
                vm.setVideo(0);
            }
        };

        vm.removeAll = function () {
            playlistService.removeAll();
            vm.reloadPlaylist();
        };
        
        function getIndexOfVideoInPlaylist(item) {
            return vm.playlist.indexOf(item);
        }
    })
    .factory('playlistService', function($localStorage) {
        $localStorage.playlist = $localStorage.playlist || [];
        return {
            playlist : function() {
                return $localStorage.playlist
            },
            add : function(item) {
                $localStorage.playlist.push(item);
            },
            remove : function (item) {
                $localStorage.playlist = _.remove($localStorage.playlist, function(elem) { return elem.id !== item.id; });
            },
            contains : function(item) {
                return angular.isObject(_.find($localStorage.playlist, {id : item.id}));
            },
            addOrRemove : function (item) {
                (this.contains(item)) ? this.remove(item) : this.add(item);
            },
            removeAll : function () {
                $localStorage.playlist = [];
            }
        };
    });