class ItemPlayerController {

    constructor(podcast, item, $timeout, deviceDetectorService) {
        this.item = item;
        this.item.podcast = podcast;
        this.$timeout = $timeout;

        this.config = {
            autoPlay: true,
            sources: [
                { src : this.item.proxyURL, type : this.item.mimeType }
            ],
            plugins: {
                controls: {
                    autoHide: !deviceDetectorService.isTouchedDevice(),
                    autoHideTime: 2000
                },
                poster: this.item.cover.url
            }
        };
    }
}

angular.module('ps.item.player', [
    'ngSanitize',
    'ngRoute',
    'com.2fdevs.videogular',
    'com.2fdevs.videogular.plugins.poster',
    'com.2fdevs.videogular.plugins.controls',
    'com.2fdevs.videogular.plugins.overlayplay',
    'com.2fdevs.videogular.plugins.buffering',

    'ps.common.component.device-detection'
])
    .config(($routeProvider) => {
        $routeProvider.
            when('/podcasts/:podcastId/item/:itemId/play', {
                templateUrl: 'item/player/item-player.html',
                controller: 'ItemPlayerController',
                controllerAs: 'ipc',
                resolve : {
                        item : (itemService, $route) => itemService.findById($route.current.params.podcastId, $route.current.params.itemId),
                        podcast : (podcastService, $route) => podcastService.findById($route.current.params.podcastId)
                    }
                });
        })
    .controller('ItemPlayerController', ItemPlayerController);