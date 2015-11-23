/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import _ from 'lodash';
import {Component, View, Module} from '../../../decorators';
import PodcastService from '../../../common/service/data/podcastService';
import TagService from '../../../common/service/data/tagService';
import NgTagsInput from '../../../common/modules/ngTagsInput';
import template from './edition.html!text';

@Module({
    name : 'ps.podcasts.details.edition',
    modules : [
        PodcastService.name,
        TagService.name,
        NgTagsInput
    ]
})
@Component({
    selector : 'podcast-edition',
    bindToController : {
        podcast : '='
    },
    as : 'pec'
})
@View({
    template : template
})
export default class PodcastEditionCtrl {
    constructor($scope, $location, tagService, podcastService) {
        "ngInject";
        this.$scope = $scope;
        this.$location = $location;
        this.tagService = tagService;
        this.podcastService = podcastService;
    }

    loadTags(query) {
        return this.tagService.search(query);
    }

    save() {
        var podcastToUpdate = _.cloneDeep(this.podcast);
        podcastToUpdate.items = null;

        return this.podcastService
            .patch(podcastToUpdate)
            .then((patchedPodcast) => _.assign(this.podcast, patchedPodcast))
            .then(() => this.$scope.$emit('podcastEdition:save'));
    }

    deletePodcast() {
        return this.podcastService
            .deletePodcast(this.podcast)
            .then(() => this.$location.path('/podcasts'));
    }
}
