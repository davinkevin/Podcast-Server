import angular from 'angular';
import AppRouteConfig from '../config/route.config';
import PodcastService from '../common/service/data/podcastService';
import TypeService from '../common/service/data/typeService';
import PodcastDetailsModule from './details/details';
import PodcastCreationModule from './creation/creation';

import PodcastsListCtrl from './pocasts.controller';

export default angular.module('ps.podcasts', [
    AppRouteConfig.name,
    PodcastDetailsModule.name,
    PodcastCreationModule.name,
    PodcastService.name,
    TypeService.name
])
    .controller(PodcastsListCtrl.name, PodcastsListCtrl)
    .config(PodcastsListCtrl.routeConfig);
