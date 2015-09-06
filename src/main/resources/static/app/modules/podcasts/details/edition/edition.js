
class podcastEditionDirective {
    constructor() {
        this.restrict = 'E';
        this.templateUrl = 'podcasts/details/edition/edition.html';
        this.scope = { podcast : '=' };
        this.controller = 'podcastEditionCtrl';
        this.controllerAs = 'pec';
        this.bindToController = true;
    }
}

class podcastEditionCtrl {
    constructor($scope, $location, tagService, podcastService) {
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

angular.module('ps.podcasts.details.edition', [
    'ps.common.service.data.podcastService',
    'ps.common.service.data.tagService',
    'ngTagsInput'
])
    .directive('podcastEdition', () => new podcastEditionDirective())
    .controller('podcastEditionCtrl', podcastEditionCtrl);
