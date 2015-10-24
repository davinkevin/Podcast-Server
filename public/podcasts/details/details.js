import EpisodesModule from './episodes/episodes';
import EditionModule from './edition/edition';
import UploadModule from './upload/upload';
import StatsModule from './stats/stats';


import PodcastDetailCtrl from './details.controller';

export default angular.module('ps.podcasts.details', [
    EpisodesModule.name,
    EditionModule.name,
    UploadModule.name,
    StatsModule.name,

    'ps.config.route',

    'ps.common.service.data.updateService'
])
    .config(PodcastDetailCtrl.routeConfig)
    .controller('PodcastDetailCtrl', PodcastDetailCtrl);