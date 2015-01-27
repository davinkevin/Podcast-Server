angular.module('ps.podcast.list', [
    'ps.config.route'
])
    .config(function($routeProvider, commonKey) {
        $routeProvider.
            when('/podcasts', {
                templateUrl: 'html/podcasts-list.html',
                controller: 'PodcastsListCtrl',
                hotkeys: commonKey,
                resolve: {
                    podcasts: function (podcastService) {
                        return podcastService.findAll();
                    }
                }
            });
    })
    .controller('PodcastsListCtrl', function ($scope, podcasts) {
        $scope.podcasts = podcasts;
    });