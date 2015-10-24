import angular from 'angular';
import AppRouteConfig from 'config/route.config';
import PodcastDetailsModule from './details/details';
import PodcastsListCtrl from './pocasts.controller';
import PodcastCreationModule from './creation/creation';

export default angular.module('ps.podcasts', [
    AppRouteConfig.name,

    PodcastDetailsModule.name,
    'ps.podcasts.creation',

    'ps.common.service.data.podcastService',
    'ps.common.service.data.typeService'
])
    .controller('PodcastsListCtrl', PodcastsListCtrl)
    .config(PodcastsListCtrl.routeConfig);
