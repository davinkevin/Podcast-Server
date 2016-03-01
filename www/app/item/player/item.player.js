/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {RouteConfig, View, Module} from '../../decorators';
import Videogular from '../../common/component/videogular/videogular';
import AppRouteConfig from '../../config/route';
import template from './item-player.html!text';

@Module({
    name : 'ps.item.player',
    modules : [ AppRouteConfig, Videogular ]
})
@RouteConfig({
    path : '/podcasts/:podcastId/item/:itemId/play',
    as : 'ipc',
    resolve : {
        item : (itemService, $route) => {"ngInject"; return itemService.findById($route.current.params.podcastId, $route.current.params.itemId);},
        podcast : (podcastService, $route) => {"ngInject"; return podcastService.findById($route.current.params.podcastId);}
    }
})
@View({
    template : template
})
export default class ItemPlayerController {

    constructor(podcast, item, VideogularService) {
        "ngInject";
        this.item = item;
        this.item.podcast = podcast;

        this.config = VideogularService
            .builder()
            .withItem(this.item)
            .build();
    }
}
