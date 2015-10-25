
import angular from 'angular';
import AppRouteConfig from 'config/route.config';
import StatsService from 'common/service/data/statsService';

import StatsController from './stats.controller';

export default angular.module('ps.stats', [
    AppRouteConfig.name,
    StatsService.name
])
    .config(StatsController.routeConfig)
    .controller('StatsController', StatsController);