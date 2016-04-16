/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../decorators';
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
@Component({
    selector : 'podcasts',
    as : 'plc',
    template : template,

    path : '/podcasts',
    resolve: {
        podcasts: (podcastService) => {"ngInject"; return podcastService.findAll(); },
        types: typeService => {"ngInject"; return typeService.findAll(); }
    }
})
export default class PodcastsListCtrl {

    constructor() { this.filters = { title : '', type : '' }; }

}
