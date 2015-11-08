/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {RouteConfig, View, HotKeys} from '../decorators';
import template from './podcasts.html!text';
import './podcasts.css!';

@RouteConfig({
    path : '/podcasts',
    as : 'plc',
    resolve: {
        podcasts: (podcastService) => {"ngInject"; return podcastService.findAll(); },
        types: typeService => {"ngInject"; return typeService.findAll(); }
    }
})
@HotKeys({})
@View({
    template : template
})
export default class PodcastsListCtrl {
    constructor(podcasts, types) {
        "ngInject";
        this.podcasts = podcasts;
        this.types = types;
        this.filters = {
            title : '',
            type : ''
        };
    }
}
