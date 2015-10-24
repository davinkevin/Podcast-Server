import angular from 'angular';
import AppRouteConfig from 'config/route.config';
import PodcastDetailsModule from './details/details';
import PodcastCreationModule from './creation/creation';
import PodcastService from 'common/service/data/podcastService'
import TypeService from 'common/service/data/typeService'

import PodcastsListCtrl from './pocasts.controller';

export default angular.module('ps.podcasts', [
    AppRouteConfig.name,
    PodcastDetailsModule.name,
    PodcastCreationModule.name,
    PodcastService.name,
    TypeService.name
])
    .controller('PodcastsListCtrl', PodcastsListCtrl)
    .config(PodcastsListCtrl.routeConfig);
