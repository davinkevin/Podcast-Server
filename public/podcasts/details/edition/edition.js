import angular from 'angular';

import PodcastService from 'common/service/data/podcastService';
import TagService from 'common/service/data/tagService';
import NgTagsInput from 'config/ngTagsInput';

import EditionComponent from './edition.component';

export default angular.module('ps.podcasts.details.edition', [
    PodcastService.name,
    TagService.name,
    NgTagsInput.name
])
    .directive('podcastEdition', EditionComponent.component)
    .controller('podcastEditionCtrl', EditionComponent);
