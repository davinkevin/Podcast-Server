angular.module('ps.player', [
    'ngSanitize',
    'ngRoute',
    'ngStorage',
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
    .controller('PlayerController', function (playlistService, $timeout) {
        var vm = this; 
        
        vm.state = null;
        vm.API = null;
        vm.currentVideo = 0;

        vm.onPlayerReady = function(API) {
            vm.API = API;

            if (vm.API.currentState == 'play' || vm.isCompleted) 
                vm.API.play();

            vm.isCompleted = false;
            vm.setVideo(0)
        };

        vm.onCompleteVideo = function() {
            vm.isCompleted = true;
            vm.currentVideo++;

            if (vm.currentVideo >= vm.playlist.length) {
                vm.currentVideo = 0;
                return;
            }

            vm.setVideo(vm.currentVideo);
        };
        

        vm.config = {
            preload : true,
            sources: [],
            theme: {
                url: "http://www.videogular.com/styles/themes/default/videogular.css"
            },
            plugins: {
                controls: {
                    autoHide: true,
                    autoHideTime: 2000
                },
                poster: ''
            }
        };
        
        vm.playlist = playlistService.playlist();

        vm.setVideo = function(index) {
            var item = vm.playlist[index];

            if (item !== null && item !== undefined) {
                vm.API.stop();
                vm.currentVideo = index;
                vm.config.sources = [{src : item.localUrl, type : item.mimeType }];
                vm.config.plugins.poster = item.cover.url;
                if (vm.config.preload) {
                    $timeout(function() { vm.API.play(); }, 500);
                }
            }
        };

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
            }
        };
    });