class PodcastsListCtrl {
    constructor(podcasts) {
        this.podcasts = podcasts;
        this.filters = {
            title : '',
            type : ''
        };
    }
}

angular.module('ps.podcast.list', [
    'ps.config.route',
    'ps.dataService.podcast'
])
    .config(($routeProvider, commonKey) => {
        $routeProvider.
            when('/podcasts', {
                templateUrl: 'html/podcasts-list.html',
                controller: 'PodcastsListCtrl',
                controllerAs: 'plc',
                hotkeys: commonKey,
                resolve: {
                    podcasts: (podcastService) => podcastService.findAll()
                }
            });
    })
    .controller('PodcastsListCtrl', PodcastsListCtrl);