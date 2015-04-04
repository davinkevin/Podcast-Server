class PodcastDetailCtrl {

    constructor($scope, podcast, UpdateService){
        this.$scope = $scope;
        this.UpdateService = UpdateService;
        this.podcast = podcast;

        this.podcastTabs= [
            { heading : 'Episodes', active : true},
            { heading : 'Edition', active : false},
            { heading : 'Upload', disabled : this.podcast.type !== 'send'}
        ];
        this.$scope.$on("podcastEdition:save", this.refreshItems);
    }

    refreshItems() {
        $scope.$broadcast('podcastItems:refresh');
    }

    refresh() {
        this.UpdateService
            .forceUpdatePodcast(this.podcast.id)
            .then(this.refreshItems);
    }

}

angular.module('ps.podcast.details', [
    'ps.config.route',
    'ps.podcast.details',
    'ps.podcast.details.episodes',
    'ps.podcast.details.edition',
    'ps.podcast.details.upload',
    'ps.dataService.updateService'
]).config(($routeProvider, commonKey) =>
    $routeProvider.
        when('/podcast/:podcastId', {
            templateUrl: 'html/podcast-detail.html',
            controller: 'PodcastDetailCtrl',
            controllerAs: 'pdc',
            hotkeys: [
                ['r', 'Refresh', 'pdc.refreshItems()'],
                ['f', 'Force Refresh', 'pdc.refresh()'],
                ['l', 'List of Items', 'pdc.podcastTabs[0].active = true'],
                ['m', 'Modification of Podcast', 'pdc.podcastTabs[1].active = true']
            ].concat(commonKey),
            resolve : { podcast : (podcastService, $route) => podcastService.findById($route.current.params.podcastId) }
        })
)
    .controller('PodcastDetailCtrl', PodcastDetailCtrl);