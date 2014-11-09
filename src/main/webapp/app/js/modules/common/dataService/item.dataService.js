/**
 * Created by kevin on 01/11/14.
 */
angular.module('ps.dataService.item', [
    'restangular'
])
    .factory('itemService', function (Restangular) {
        'use strict';
        return {
            search: search,
            findById : findById,
            getItemForPodcastWithPagination : getItemForPodcastWithPagination,
            restangularizePodcastItem : restangularizePodcastItem
        };

        function search(searchParameters) {
            //{term : 'term', tags : $scope.searchTags, size: numberByPage, page : $scope.currentPage - 1, direction : $scope.direction, properties : $scope.properties}
            return Restangular.one("item/search")
                .post(null, searchParameters)
                .then(function (responseFromServer) {
                    responseFromServer.content = restangularizedItems(responseFromServer.content);
                    return responseFromServer;
                });
        }

        function findById(podcastId, itemId) {
            return Restangular.one("podcast", podcastId).one("items", itemId).get();
        }

        function getItemForPodcastWithPagination(podcast, pageParemeters) {
            return podcast.one("items").post(null, pageParemeters);
        }

        function restangularizePodcastItem (podcast, items) {
            return Restangular.restangularizeCollection(podcast, items, 'items');
        }

        // Private Function :

        // transformation
        function restangularizedItems(itemList) {
            var restangularList = [];
            angular.forEach(itemList, function (value) {
                restangularList.push(Restangular.restangularizeElement(Restangular.one('podcast', value.podcastId), value, 'items'));
            });
            return restangularList;
        }
    });
