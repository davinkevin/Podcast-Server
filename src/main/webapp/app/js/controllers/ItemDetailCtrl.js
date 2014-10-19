angular.module('podcast.controller')
    .controller('ItemDetailCtrl', function ($scope, $routeParams, $http, Restangular, podcastWebSocket, DonwloadManager, $location, $q) {

        var idItem = $routeParams.itemId,
            idPodcast = $routeParams.podcastId,
            basePodcast = Restangular.one("podcast", idPodcast),
            baseItem = basePodcast.one("items", idItem),
            webSockedUrl = "/topic/podcast/".concat($scope.item.podcast.id);


        $q.all([basePodcast.get(), baseItem.get()]).then(function (arrayOfResult) {
            $scope.item = arrayOfResult[1];
            $scope.item.podcast = arrayOfResult[0];
        }).then(function () {

            podcastWebSocket
                .subscribe(webSockedUrl, function(message) {
                    var itemFromWS = JSON.parse(message.body);

                    if (itemFromWS.id == $scope.item.id) {
                        _.assign($scope.item, itemFromWS);
                    }
                });

            $scope.$on('$destroy', function () {
                podcastWebSocket.unsubscribe(webSockedUrl);
            });
        });


        $scope.remove = function(item) {
            return item.remove().then(function() {
                $location.path('/podcast/'.concat($scope.item.podcast.id));
            });
        };

        $scope.reset = function (item) {
            return item.reset().then(function (itemReseted) {
                _.assign($scope.item, itemReseted);
            });
        };

        $scope.download = DonwloadManager.download;
        $scope.stopDownload = DonwloadManager.stopDownload;
        $scope.toggleDownload = DonwloadManager.toggleDownload;

    });