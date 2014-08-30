angular.module('podcast.controller')
    .controller('PodcastDetailCtrl', function ($scope, podcast, $routeParams, Restangular, ngstomp, $log, $location) {

        $scope.podcast = podcast;

        function refreshItems () {
            $scope.$broadcast('podcastItems:refresh');
        }

        $scope.refresh = function () {
            Restangular.one("task").customPOST($scope.podcast.id, "updateManager/updatePodcast/force")
                .then(refreshItems);
        };
        $scope.$on("podcastEdition:save", refreshItems);

    });