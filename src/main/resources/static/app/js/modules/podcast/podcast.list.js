class PodcastsListCtrl {
    constructor(podcasts, types) {
        this.podcasts = podcasts;
        this.types = types;
        this.filters = {
            title : '',
            type : ''
        };
    }
}

angular.module('ps.podcast.list', [
    'ps.config.route',
    'ps.dataService.podcast',
    'ps.dataService.type'
])
    .config(($routeProvider, commonKey) => {
        $routeProvider.
            when('/podcasts', {
                templateUrl: 'html/podcasts-list.html',
                controller: 'PodcastsListCtrl',
                controllerAs: 'plc',
                hotkeys: commonKey,
                resolve: {
                    podcasts: (podcastService) => podcastService.findAll(),
                    types: typeService => typeService.findAll()
                }
            });
    })
    .controller('PodcastsListCtrl', PodcastsListCtrl);