angular.module('ps.item.details', [
    'restangular',
    'ps.websocket',
    'ps.dataService.donwloadManager'
])
    .controller('ItemDetailCtrl', function ($scope, $routeParams, Restangular, podcastWebSocket, DonwloadManager, $location, $q) {

        var idItem = $routeParams.itemId,
            idPodcast = $routeParams.podcastId,
            basePodcast = Restangular.one("podcast", idPodcast),
            baseItem = basePodcast.one("items", idItem);



        $q.all([basePodcast.get(), baseItem.get()]).then(function (arrayOfResult) {
            $scope.item = arrayOfResult[1];
            $scope.item.podcast = arrayOfResult[0];
        }).then(function () {
            var webSockedUrl = "/topic/podcast/".concat($scope.item.podcast.id);

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