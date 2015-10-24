import angular from 'angular';

import AppRouteConfig from 'config/route.config';
import PodcastDetailsModule from './details/details.js';

import PodcastsListCtrl from './pocasts.controller';


export default angular.module('ps.podcasts', [
    AppRouteConfig.name,

    PodcastDetailsModule.name,
    'ps.podcasts.creation',

    'ps.common.service.data.podcastService',
    'ps.common.service.data.typeService'
])
    .controller('PodcastsListCtrl', PodcastsListCtrl)
    .config(PodcastsListCtrl.routeConfig);
