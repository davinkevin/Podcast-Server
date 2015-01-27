angular.module('ps.podcast.details', [
    'ps.config.route',
    'ps.podcast.details',
    'ps.podcast.details.episodes',
    'ps.podcast.details.edition',
    'ps.podcast.details.upload',
    'restangular'
]).config(function($routeProvider, commonKey) {
    $routeProvider.
        when('/podcast/:podcastId', {
            templateUrl: 'html/podcast-detail.html',
            controller: 'PodcastDetailCtrl',
            hotkeys: [
                ['r', 'Refresh', 'refreshItems()'],
                ['f', 'Force Refresh', 'refresh()'],
                ['l', 'List of Items', 'tabs[0].active = true'],
                ['m', 'Modification of Podcast', 'tabs[1].active = true']
            ].concat(commonKey),
            resolve : {
                podcast : function (Restangular, $route) {
                    return Restangular.one('podcast', $route.current.params.podcastId).get();
                }
            }
        })    
})
    .controller('PodcastDetailCtrl', function ($scope, podcast, $routeParams, Restangular) {

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