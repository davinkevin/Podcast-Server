/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../decorators';
import {TitleService} from '../common/service/title.service';
import AppRouteConfig from '../config/route';
import PodcastService from '../common/service/data/podcastService';
import TypeService from '../common/service/data/typeService';
import PodcastDetailsModule from './details/details';
import PodcastCreationModule from './creation/creation';
import template from './podcasts.html!text';
import './podcasts.css!';

@Module({
    name : 'ps.podcasts',
    modules : [PodcastCreationModule, PodcastDetailsModule, PodcastService, AppRouteConfig, TypeService, TitleService]
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
export default class PodcastsListComponent {

    static defaultFilters = { title: '', type: '' };

    constructor(TitleService, $sessionStorage) {
        "ngInject";
        this.$sessionStorage = $sessionStorage;
        this.TitleService = TitleService;
    }

    $onInit(){
        this.TitleService.title = 'Podcasts';
        this.filters = PodcastsListComponent.defaultFilters;
    }

    get filters() {
        return this.$sessionStorage.podcasts;
    }

    set filters(val) {
        this.$sessionStorage.podcasts = this.$sessionStorage.podcasts || val;
    }

}
