angular.module('ps.item.player', [
    'ngSanitize',
    'ngRoute',
    'com.2fdevs.videogular'
])
    .config(function($routeProvider) {
        $routeProvider.
            when('/podcast/:podcastId/item/:itemId/play', {
                templateUrl: 'html/item-player.html',
                controller: 'ItemPlayerController',
                controllerAs: 'ipc',
                resolve : {
                    item : function (itemService, $route) {
                        return itemService.findById($route.current.params.podcastId, $route.current.params.itemId);
                    },
                    podcast : function (podcastService, $route) {
                        return podcastService.findById($route.current.params.podcastId);
                    }
                }
            });
    })
    .controller('ItemPlayerController', function (podcast, item) {
        var vm = this;
        
        vm.item = item;
        vm.item.podcast = podcast;

        vm.config = {
            preload: 'none',
            sources: [
                { src : item.localUrl, type : item.mimeType }
            ],
            theme: {
                url: "http://www.videogular.com/styles/themes/default/videogular.css"
            }
        }
    });