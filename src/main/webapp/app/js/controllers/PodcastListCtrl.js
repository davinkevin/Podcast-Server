angular.module('podcast.controller')
    .controller('PodcastsListCtrl', function ($scope, Restangular, localStorageService) {

        $scope.podcasts = localStorageService.get('podcastslist');
        Restangular.all("podcast").getList().then(function(podcasts) {
            $scope.podcasts = podcasts;
            localStorageService.add('podcastslist', podcasts);
        });
    })