'use strict';

angular.module('podcast.details.edition', [])
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
    .controller('podcastEditionCtrl', function ($scope, Restangular, $location) {
        var tags = Restangular.all("tag");

        $scope.loadTags = function (query) {
            return tags.post(null, {name : query});
        };

        $scope.save = function () {
            var podcastToUpdate = _.cloneDeep($scope.podcast);
            podcastToUpdate.items = null;
            $scope.podcast.patch(podcastToUpdate)
                .then(function (patchedPodcast){
                    _.assign($scope.podcast, patchedPodcast);
                })
                .then(function () {
                    $scope.$emit('podcastEdition:save');
                });
        };
        $scope.deletePodcast = function () {
            $scope.podcast.remove().then(function () {
                $location.path('/podcasts');
            });
        };
    });
