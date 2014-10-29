'use strict';

angular.module('podcast.details.episodes', [
    'podcast.websocket'
])
    .directive('podcastItemsList', function($log){
        return {
            restrcit : 'E',
            templateUrl : 'html/podcast-details-episodes.html',
            scope : {
                podcast : '='
            },
            controller : 'podcastItemsListCtrl'
        };
    })
    .constant('PodcastItemPerPage', 10)
    .controller('podcastItemsListCtrl', function ($scope, Restangular, ngstomp, DonwloadManager, PodcastItemPerPage, podcastWebSocket ) {
        $scope.currentPage = 1;
        $scope.itemPerPage = PodcastItemPerPage;

        var webSocketUrl = "/topic/podcast/".concat($scope.podcast.id);

        podcastWebSocket.subscribe(webSocketUrl, function (message) {
            var item = JSON.parse(message.body);
            var elemToUpdate = _.find($scope.podcast.items, { 'id': item.id });
            _.assign(elemToUpdate, item);
        });

        $scope.$on('$destroy', function () {
            podcastWebSocket.unsubscribe(webSocketUrl);
        });

        function restangularizedItems(itemList) {
            var restangularList = [];
            angular.forEach(itemList, function (value) {
                restangularList.push(Restangular.restangularizeElement(Restangular.one('podcast', value.podcastId), value, 'items'));
            });
            return restangularList;
        }


        $scope.loadPage = function() {
            $scope.currentPage = ($scope.currentPage < 1) ? 1 : ($scope.currentPage > Math.ceil($scope.totalItems / PodcastItemPerPage)) ? Math.ceil($scope.totalItems / PodcastItemPerPage) : $scope.currentPage;
            return $scope.podcast.one("items").post(null, {size: PodcastItemPerPage, page : $scope.currentPage - 1, direction : 'DESC', properties : 'pubdate'})
                .then(function(itemsResponse) {
                    $scope.podcast.items = restangularizedItems(itemsResponse.content);
                    $scope.podcast.totalItems = itemsResponse.totalElements;
                });
        };

        $scope.loadPage();
        $scope.$on("podcastItems:refresh", function () {
            $scope.currentPage = 1;
            $scope.loadPage();
        });

        $scope.remove = function (item) {
            item.remove().then(function() {
                $scope.podcast.items = _.reject($scope.podcast.items, function(elem) {
                    return (elem.id === item.id);
                });
            });
        };
        $scope.reset = function (item) {
            return item.reset().then(function (itemReseted) {
                var itemInList = _.find($scope.podcast.items, { 'id': itemReseted.id });
                _.assign(itemInList, itemReseted);
            });
        };

        $scope.swipePage = function(val) {
            $scope.currentPage += val;
            $scope.loadPage();
        };

        $scope.stopDownload = DonwloadManager.stopDownload;
        $scope.toggleDownload = DonwloadManager.toggleDownload;
    });
