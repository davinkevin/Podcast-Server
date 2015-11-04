/**
    * Created by kevin on 25/10/2015 for PodcastServer
    */
import _ from 'lodash';
import template from './edition.html!text';

export default class podcastEditionCtrl {
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
    
    static component() {
        return {
            restrict : 'E',
            template : template,
            scope : { podcast : '=' },
            controller : 'podcastEditionCtrl',
            controllerAs : 'pec',
            bindToController : true
        };
    }
}
