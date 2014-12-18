angular.module('ps.search.item', [
    'ps.dataService.item',
    'ps.dataService.tag',
    'ps.dataService.donwloadManager'
])
    .constant('ItemPerPage', 12)
    .controller('ItemsSearchCtrl', function ($scope, $cacheFactory, $location, itemService, tagService, podcastWebSocket, DonwloadManager, ItemPerPage) {
        'use strict';

        // Gestion du cache de la pagination :
        var cache = $cacheFactory.get('paginationCache') || $cacheFactory('paginationCache');

        $scope.changePage = function() {
            $scope.searchParameters.page = ($scope.currentPage <= 1) ? 1 : ($scope.currentPage > Math.ceil($scope.totalItems / ItemPerPage)) ? Math.ceil($scope.totalItems / ItemPerPage) : $scope.currentPage;
            $scope.searchParameters.page -= 1;
            itemService.search($scope.searchParameters).then(function(itemsResponse) {

                $scope.items = itemsResponse.content;
                $scope.totalPages = itemsResponse.totalPages;
                $scope.totalItems = itemsResponse.totalElements;

                cache.put('search:currentPage', $scope.currentPage);
                cache.put('search:currentWord', $scope.term);
                cache.put('search:currentTags', $scope.searchTags);
                cache.put("search:direction", $scope.direction);
                cache.put("search:properties", $scope.properties);

                $location.search("page", $scope.currentPage);
            });
        };

        $scope.$on('$routeUpdate', function(){
            if ($scope.currentPage !== $location.search().page) {
                $scope.currentPage = $location.search().page || 1;
                $scope.changePage();
            }
        });

        $scope.swipePage = function(val) {
            $scope.currentPage += val;
            $scope.changePage();
        };

        //** Item Operation **//
        $scope.remove = function (item) {
            return item.remove().then(function(){
                return $scope.changePage();
            });
        };

        $scope.reset = function (item) {
            return item.reset().then(function (itemReseted) {
                var itemInList = _.find($scope.items, { 'id': itemReseted.id });
                _.assign(itemInList, itemReseted);
            });
        };

        // Longeur inconnu au chargement :
        //{term : 'term', tags : $scope.searchTags, size: numberByPage, page : $scope.currentPage - 1, direction : $scope.direction, properties : $scope.properties}
        $scope.totalItems = Number.MAX_VALUE;
        $scope.maxSize = 10;

        $scope.searchParameters = {};
        $scope.searchParameters.size = ItemPerPage;
        $scope.currentPage = cache.get("search:currentPage") || 1;
        $scope.searchParameters.term = cache.get("search:currentWord") || undefined;
        $scope.searchParameters.searchTags = cache.get("search:currentTags") || undefined;
        $scope.searchParameters.direction = cache.get("search:direction") || undefined;
        $scope.searchParameters.properties = cache.get("search:properties") || undefined;

        $scope.changePage();

        //** DownloadManager **//
        $scope.stopDownload = DonwloadManager.stopDownload;
        $scope.toggleDownload = DonwloadManager.toggleDownload;
        $scope.loadTags = tagService.search;

        //** WebSocket Subscription **//
        var webSocketUrl = "/topic/download";
        podcastWebSocket.subscribe(webSocketUrl, updateItemFromWS);

        $scope.$on('$destroy', function () {
            podcastWebSocket.unsubscribe(webSocketUrl);
        });

        function updateItemFromWS(message) {
            var item = JSON.parse(message.body);

            var elemToUpdate = _.find($scope.items, { 'id': item.id });
            if (elemToUpdate)
                _.assign(elemToUpdate, item);
        }

    });