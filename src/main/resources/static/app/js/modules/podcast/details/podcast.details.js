class PodcastDetailCtrl {

    constructor($scope, podcast, UpdateService){
        this.$scope = $scope;
        this.UpdateService = UpdateService;
        this.podcast = podcast;

        this.podcastTabs= [
            { heading : 'Episodes', active : true},
            { heading : 'Edition', active : false},
            { heading : 'Upload', disabled : this.podcast.type !== 'send'},
            { heading : 'Stats', active : false}
        ];
        this.$scope.$on("podcastEdition:save", () => this.refreshItems());
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

}

angular.module('ps.podcast.details', [
    'ps.config.route',
    'ps.podcast.details',
    'ps.podcast.details.episodes',
    'ps.podcast.details.edition',
    'ps.podcast.details.upload',
    'ps.podcast.details.stats',
    'ps.dataService.updateService'
]).config(($routeProvider, commonKey) =>
    $routeProvider.
        when('/podcasts/:podcastId', {
            templateUrl: 'html/podcast-detail.html',
            controller: 'PodcastDetailCtrl',
            controllerAs: 'pdc',
            /*hotkeys: [
                ['r', 'Refresh', 'pdc.refreshItems()'],
                ['f', 'Force Refresh', 'pdc.refresh()'],
                ['l', 'List of Items', 'pdc.tabsActive(0)'],
                ['m', 'Modification of Podcast', 'pdc.tabsActive(1)']
            ].concat(commonKey),*/
            hotkeys : commonKey,
            resolve : { podcast : (podcastService, $route) => podcastService.findById($route.current.params.podcastId) }
        })
)
    .controller('PodcastDetailCtrl', PodcastDetailCtrl);