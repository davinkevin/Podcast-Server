/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {RouteConfig, View, HotKeys, Module} from '../../decorators';
import Videogular from '../../common/modules/videogular';
import AppRouteConfig from '../../config/route.config';
import DeviceDetectionService from '../../common/service/device-detection';
import template from './item-player.html!text';

@Module({
    name : 'ps.item.player',
    modules : [ AppRouteConfig.name, Videogular, DeviceDetectionService ]
})
@RouteConfig({
    path : '/podcasts/:podcastId/item/:itemId/play',
    as : 'ipc',
    resolve : {
        item : (itemService, $route) => {"ngInject"; return itemService.findById($route.current.params.podcastId, $route.current.params.itemId);},
        podcast : (podcastService, $route) => {"ngInject"; return podcastService.findById($route.current.params.podcastId);}
    }
})
@HotKeys({})
@View({
    template : template
})
export default class ItemPlayerController {

    constructor(podcast, item, $timeout, deviceDetectorService) {
        "ngInject";
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
