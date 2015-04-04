angular.module('ps.podcast.details.edition', [
    'ps.dataService.podcast',
    'ps.dataService.tag',
    'ngTagsInput'
])
    .directive('podcastEdition', function () {
        return {
            restrcit : 'E',
            templateUrl : 'html/podcast-details-edition.html',
            scope : {
                podcast : '='
            },
            controller : 'podcastEditionCtrl'
        };
    })
    .controller('podcastEditionCtrl', function ($scope, $location, tagService, podcastService) {
        $scope.loadTags = function (query) {
            return tagService.search(query);
        };

        $scope.save = function () {
            var podcastToUpdate = _.cloneDeep($scope.podcast);
            podcastToUpdate.items = null;

            podcastService.patch(podcastToUpdate).then(function (patchedPodcast){
                _.assign($scope.podcast, patchedPodcast);
            }).then(function () {
                $scope.$emit('podcastEdition:save');
            });
        };

        $scope.deletePodcast = function () {
            podcastService.deletePodcast($scope.podcast).then(function () {
                $location.path('/podcasts');
            });
        };
    });
