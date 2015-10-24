import EpisodesComponent from './episodes/episodes';
import EditionComponent from './edition/edition';
import UploadComponent from './upload/upload';

import PodcastDetailCtrl from './details.controller';

export default angular.module('ps.podcasts.details', [
    EpisodesComponent.name,
    EditionComponent.name,
    'ps.podcasts.details.upload',
    'ps.podcasts.details.stats',

    'ps.config.route',

    'ps.common.service.data.updateService'
])
    .config(PodcastDetailCtrl.routeConfig)
    .controller('PodcastDetailCtrl', PodcastDetailCtrl);