angular.module('ps.item.details', [
    'restangular',
    'ps.websocket',
    'ps.dataService.donwloadManager'
])
    .controller('ItemDetailCtrl', function ($scope, podcastWebSocket, DonwloadManager, $location, podcast, item) {

        $scope.item = item;
        $scope.item.podcast = podcast;
        $scope.download = DonwloadManager.download;
        $scope.stopDownload = DonwloadManager.stopDownload;
        $scope.toggleDownload = DonwloadManager.toggleDownload;


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

        //** WebSocket Inscription **//
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