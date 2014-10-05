angular.module('podcast.controller')
    .controller('ItemDetailCtrl', function ($scope, $routeParams, $http, Restangular, ngstomp, DonwloadManager, $location, $q) {

        var idItem = $routeParams.itemId,
            idPodcast = $routeParams.podcastId,
            basePodcast = Restangular.one("podcast", idPodcast);
            baseItem = basePodcast.one("items", idItem);


        /*basePodcast.get().then(function (podcastFromServer) {
            $scope.podcast = podcastFromServer;
            return $scope.podcast.one("items", idItem).get();
        }).then(function (itemFromServer) {
            $scope.item = itemFromServer;
            itemFromServer.podcast = $scope.podcast;
            return itemFromServer;
        })*/
        $q.all([basePodcast.get(), baseItem.get()]).then(function (arrayOfResult) {
            $scope.item = arrayOfResult[1];
            $scope.item.podcast = arrayOfResult[0];
        }).then(function () {
            $scope.wsClient = ngstomp("/ws", SockJS);
            $scope.wsClient.connect("user", "password", function(){
                $scope.wsClient.subscribe("/topic/podcast/" + $scope.item.podcast.id, function(message) {
                    var itemFromWS = JSON.parse(message.body);

                    if (itemFromWS.id == $scope.item.id) {
                        _.assign($scope.item, itemFromWS);
                    }
                });
            });
            $scope.$on('$destroy', function () {
                $scope.wsClient.disconnect(function(){});
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