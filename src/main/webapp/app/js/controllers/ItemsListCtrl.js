angular.module('podcast.controller')
    .controller('ItemsListCtrl', function ($scope, $http, $routeParams, $cacheFactory, Restangular, ngstomp, DonwloadManager, $log, $location) {

        // Gestion du cache de la pagination :
        var cache = $cacheFactory.get('paginationCache') || $cacheFactory('paginationCache'),
            numberByPage = 12;

        //$scope.selectPage = function (pageNo) {
        $scope.changePage = function() {
            $scope.currentPage = ($scope.currentPage < 1) ? 1 : ($scope.currentPage > Math.ceil($scope.totalItems / numberByPage)) ? Math.ceil($scope.totalItems / numberByPage) : $scope.currentPage;
            Restangular.one("item/pagination").get({size: numberByPage, page : $scope.currentPage - 1, direction : 'DESC', properties : 'pubdate'}).then(function(itemsResponse) {
                $scope.items = itemsResponse.content;
                $scope.totalItems = parseInt(itemsResponse.totalElements);
                cache.put('currentPage', $scope.currentPage);
                $location.search("page", $scope.currentPage);
            });
        };

        $scope.$on('$routeUpdate', function(){
            if ($scope.currentPage !== $location.search().page) {
                $scope.currentPage = $location.search().page;
                $scope.changePage();
            }
        });

        // Longeur inconnu au chargement :
        $scope.totalItems = Number.MAX_VALUE;
        $scope.maxSize = 10;
        $scope.currentPage = cache.get("currentPage") || 1;
        $scope.changePage();

        $scope.download = DonwloadManager.download;
        $scope.stopDownload = DonwloadManager.stopDownload;
        $scope.toggleDownload = DonwloadManager.toggleDownload;

        $scope.wsClient = ngstomp('/download', SockJS);
        $scope.wsClient.connect("user", "password", function(){
            $scope.wsClient.subscribe("/topic/download", function(message) {
                var item = JSON.parse(message.body);

                var elemToUpdate = _.find($scope.items, { 'id': item.id });
                if (elemToUpdate)
                    _.assign(elemToUpdate, item);
            });
        });
        $scope.$on('$destroy', function () {
            $scope.wsClient.disconnect(function(){});
        });

    });