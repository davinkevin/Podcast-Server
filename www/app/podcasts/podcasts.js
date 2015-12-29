/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {RouteConfig, View, Module} from '../decorators';
import AppRouteConfig from '../config/route';
import PodcastService from '../common/service/data/podcastService';
import TypeService from '../common/service/data/typeService';
import PodcastDetailsModule from './details/details';
import PodcastCreationModule from './creation/creation';
import template from './podcasts.html!text';
import './podcasts.css!';

@Module({
    name : 'ps.podcasts',
    modules : [
        PodcastDetailsModule,
        PodcastCreationModule,
        PodcastService,
        AppRouteConfig,
        TypeService
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
@View({
    template : template
})
export default class PodcastsListCtrl {
    constructor(podcasts, types) {
        "ngInject";
        this.podcasts = podcasts;
        this.types = types;
        this.filters = { title : '', type : '' };
    }
}
