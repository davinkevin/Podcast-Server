class PodcastDetailCtrl {

    constructor($scope, podcast, UpdateService, $timeout){
        this.$scope = $scope;
        this.UpdateService = UpdateService;
        this.podcast = podcast;
        this.$timeout = $timeout;

        this.podcastTabs = [
            { heading : 'Episodes', active : true},
            { heading : 'Edition', active : false},
            { heading : 'Upload', disabled : this.podcast.type !== 'send'},
            { heading : 'Stats', active : false }
        ];
        this.$scope.$on("podcastEdition:save", () => this.refreshItems());
        this.$scope.$on("podcastEdition:upload", () => this.refreshItems());
    }

    refreshItems() {
        this.$scope.$broadcast('podcastItems:refresh');
    }

    refresh() {
        this.UpdateService
            .forceUpdatePodcast(this.podcast.id)
            .then(() => this.refreshItems());
    }

    tabsActive(num) {
        this.podcastTabs[num].active = true;
    }

    chartReflow() {
        this.$timeout(() => {
            this.$scope.$broadcast('highchartsng.reflow');
        }, 10);
    }

}

angular.module('ps.podcasts.details', [
    'ps.podcasts.details.episodes',
    'ps.podcasts.details.edition',
    'ps.podcasts.details.upload',
    'ps.podcasts.details.stats',

    'ps.config.route',

    'ps.common.service.data.updateService'
])
    .config(($routeProvider, commonKey) =>
        $routeProvider.
            when('/podcasts/:podcastId', {
                templateUrl: 'podcasts/details/details.html',
                controller: 'PodcastDetailCtrl',
                controllerAs: 'pdc',
                hotkeys : commonKey,
                resolve : {
                    podcast : (podcastService, $route) => podcastService.findById($route.current.params.podcastId)
                }
            })
)
    .controller('PodcastDetailCtrl', PodcastDetailCtrl);