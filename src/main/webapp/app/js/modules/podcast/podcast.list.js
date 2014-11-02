angular.module('ps.podcast.list', [
])
    .controller('PodcastsListCtrl', function ($scope, podcasts) {
        $scope.podcasts = podcasts;
    });