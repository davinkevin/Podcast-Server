import EpisodesComponent from './episodes/episodes';

import PodcastDetailCtrl from './details.controller';

export default angular.module('ps.podcasts.details', [
    EpisodesComponent.name,
    'ps.podcasts.details.edition',
    'ps.podcasts.details.upload',
    'ps.podcasts.details.stats',

    'ps.config.route',

    'ps.common.service.data.updateService'
])
    .config(PodcastDetailCtrl.routeConfig)
    .controller('PodcastDetailCtrl', PodcastDetailCtrl);