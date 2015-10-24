import angular from 'angular';
import AppRouteConfig from 'config/route.config';
import NgTagsInput from 'config/ngTagsInput';
import PodcastService from 'common/service/data/podcastService';
import TypeService from 'common/service/data/typeService';
import TagService from 'common/service/data/tagService';

import PodcastCreationCtrl from './creation.controller';

export default angular.module('ps.podcasts.creation', [
    AppRouteConfig.name,
    NgTagsInput.name,
    PodcastService.name,
    TypeService.name,
    TagService.name
])
    .config(PodcastCreationCtrl.routeConfig)
    .constant('defaultPodcast', { hasToBeDeleted : true, cover : { height: 200, width: 200 } })
    .controller('PodcastAddCtrl', PodcastCreationCtrl);