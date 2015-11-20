/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {RouteConfig, View, HotKeys, Module} from '../decorators';
import AppRouteConfig from '../config/route.config';
import PodcastService from '../common/service/data/podcastService';
import TypeService from '../common/service/data/typeService';
import PodcastDetailsModule from './details/details';
import PodcastCreationModule from './creation/creation';
import template from './podcasts.html!text';
import './podcasts.css!';

@Module({
    name : 'ps.podcasts',
    modules : [
        AppRouteConfig.name,
        PodcastDetailsModule.name,
        PodcastCreationModule.name,
        PodcastService.name,
        TypeService.name
    ]
})
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
