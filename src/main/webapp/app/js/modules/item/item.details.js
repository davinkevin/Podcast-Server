angular.module('ps.item.details', [
    'ps.dataService.donwloadManager',
    'ps.player'
]).config(function($routeProvider, commonKey) {
    $routeProvider.
        when('/podcast/:podcastId/item/:itemId', {
            templateUrl: 'html/item-detail.html',
            controller: 'ItemDetailCtrl',
            hotkeys: commonKey,
            resolve : {
                item : function (itemService, $route) {
                    return itemService.findById($route.current.params.podcastId, $route.current.params.itemId);
                },
                podcast : function (podcastService, $route) {
                    return podcastService.findById($route.current.params.podcastId);
                }
            }
        });
})
    .controller('ItemDetailCtrl', function ($scope, DonwloadManager, $location, playlistService, podcast, item) {

        $scope.item = item;
        $scope.item.podcast = podcast;
        
        $scope.stopDownload = DonwloadManager.ws.stop;
        $scope.toggleDownload = DonwloadManager.ws.toggle;


        $scope.remove = function(item) {
            return item.remove().then(function() {
                playlistService.remove(item);
                $location.path('/podcast/'.concat($scope.item.podcast.id));
            });
        };

        $scope.reset = function (item) {
            return item.reset().then(function (itemReseted) {
                _.assign($scope.item, itemReseted);
                playlistService.remove(item);
            });
        };
        
        $scope.toggleInPlaylist = function () {
            playlistService.addOrRemove(item);
        };
        
        $scope.isInPlaylist = function() {
            return playlistService.contains(item);
        };

        //** WebSocket Inscription **//
        var webSockedUrl = "/topic/podcast/".concat($scope.item.podcast.id);

        DonwloadManager
            .ws
                .subscribe(webSockedUrl, function(message) {
                    var itemFromWS = JSON.parse(message.body);
    
                    if (itemFromWS.id == $scope.item.id) {
                        _.assign($scope.item, itemFromWS);
                    }
                }, $scope);
    });