import angular from 'angular';
import EpisodesModule from './episodes/episodes';
import EditionModule from './edition/edition';
import UploadModule from './upload/upload';
import StatsModule from './stats/stats';
import AppRouteConfig from '../../config/route.config';
import UpdateService from '../../common/service/data/updateService';
import PodcastDetailCtrl from './details.controller';

export default angular.module('ps.podcasts.details', [
    EpisodesModule.name,
    EditionModule.name,
    UploadModule.name,
    StatsModule.name,
    AppRouteConfig.name,
    UpdateService.name
])
    .config(PodcastDetailCtrl.routeConfig)
    .controller(PodcastDetailCtrl.name, PodcastDetailCtrl);