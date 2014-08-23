angular.module('podcast.controller')
    .controller('ItemDetailCtrl', function ($scope, $routeParams, $http, Restangular, ngstomp, DonwloadManager, $location) {

        var idItem = $routeParams.itemId;

        Restangular.one("item", idItem).get().then(function(item) {
            $scope.item = item;
        }).then(function() {
            $scope.item.one("podcast").get().then(function(podcast) {

                $scope.item.podcast = podcast;

                $scope.wsClient = ngstomp("/download", SockJS);
                $scope.wsClient.connect("user", "password", function(){
                    $scope.wsClient.subscribe("/topic/podcast/" + podcast.id, function(message) {
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

        });

        $scope.remove = function(item) {
            return item.remove().then(function() {
                $location.path('/podcast/'.concat($scope.item.podcast.id));
            });
        };

        $scope.download = DonwloadManager.download;
        $scope.stopDownload = DonwloadManager.stopDownload;
        $scope.toggleDownload = DonwloadManager.toggleDownload;

    });