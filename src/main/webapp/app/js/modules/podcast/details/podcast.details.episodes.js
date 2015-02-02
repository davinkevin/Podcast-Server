'use strict';

angular.module('ps.podcast.details.episodes', [
    'ps.player'
])
    .directive('podcastItemsList', function(){
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
    .controller('podcastItemsListCtrl', function ($scope, DonwloadManager, PodcastItemPerPage, itemService, playlistService ) {
        $scope.currentPage = 1;
        $scope.itemPerPage = PodcastItemPerPage;

        var webSocketUrl = "/topic/podcast/".concat($scope.podcast.id);

        DonwloadManager
            .ws
                .subscribe(webSocketUrl, function (message) {
                    var item = JSON.parse(message.body);
                    var elemToUpdate = _.find($scope.podcast.items, { 'id': item.id });
                    _.assign(elemToUpdate, item);
                }, $scope);

        $scope.loadPage = function() {
            $scope.currentPage = ($scope.currentPage < 1) ? 1 : ($scope.currentPage > Math.ceil($scope.totalItems / PodcastItemPerPage)) ? Math.ceil($scope.totalItems / PodcastItemPerPage) : $scope.currentPage;
            return itemService.getItemForPodcastWithPagination($scope.podcast, {size: PodcastItemPerPage, page : $scope.currentPage - 1, direction : 'DESC', properties : 'pubdate'})
                .then(function(itemsResponse) {
                    $scope.podcast.items = itemService.restangularizePodcastItem($scope.podcast, itemsResponse.content);
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
            }).then(function() {
                playlistService.remove(item);
            }).then($scope.loadPage);
        };


        $scope.reset = function (item) {
            return item.reset().then(function (itemReseted) {
                var itemInList = _.find($scope.podcast.items, { 'id': itemReseted.id });
                _.assign(itemInList, itemReseted);
                playlistService.remove(itemInList);
            });
        };

        $scope.addOrRemoveInPlaylist = function (item) {
            playlistService.addOrRemove(item);
        };

        $scope.isInPlaylist = function(item) {
            return playlistService.contains(item);
        };

        $scope.swipePage = function(val) {
            $scope.currentPage += val;
            $scope.loadPage();
        };

        $scope.stopDownload = DonwloadManager.ws.stop;
        $scope.toggleDownload = DonwloadManager.ws.toggle;
    });
