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

angular.module('ps.podcasts', [
    'ps.podcasts.details',
    'ps.podcasts.creation',

    'ps.config.route',

    'ps.common.service.data.podcastService',
    'ps.common.service.data.typeService'
])
    .config(($routeProvider, commonKey) => {
        $routeProvider.
            when('/podcasts', {
                templateUrl: 'podcasts/list.html',
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