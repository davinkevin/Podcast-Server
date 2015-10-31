/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */

import template from './item-player.html!text';

export default class ItemPlayerController {

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

    static routeConfig($routeProvider) {
        $routeProvider.
            when('/podcasts/:podcastId/item/:itemId/play', {
                template: template,
                controller: 'ItemPlayerController',
                controllerAs: 'ipc',
                resolve : {
                    item : (itemService, $route) => itemService.findById($route.current.params.podcastId, $route.current.params.itemId),
                    podcast : (podcastService, $route) => podcastService.findById($route.current.params.podcastId)
                }
            })
    }
}
