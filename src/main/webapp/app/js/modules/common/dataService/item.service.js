/**
 * Created by kevin on 01/11/14.
 */
angular.module('ps.dataService.item', [
    'restangular'
])
    .factory('itemService', function (Restangular) {
        'use strict';
        return {
            search: search
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
