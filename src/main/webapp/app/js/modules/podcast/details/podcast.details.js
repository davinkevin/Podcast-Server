angular.module('ps.podcast.details', [
    'ps.podcast.details',
    'ps.podcast.details.episodes',
    'ps.podcast.details.edition',
    'ps.podcast.details.upload',
    'restangular'
]).controller('PodcastDetailCtrl', function ($scope, podcast, $routeParams, Restangular) {

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