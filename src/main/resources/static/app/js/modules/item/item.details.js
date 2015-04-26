class ItemDetailCtrl {

    constructor($scope, DonwloadManager, $location, playlistService, podcast, item){
        this.item = item;
        this.$location = $location;
        this.item.podcast = podcast;
        this.playlistService = playlistService;
        this.DonwloadManager = DonwloadManager;

        //** WebSocket Inscription **//
        let webSockedUrl = "/topic/podcast/".concat(this.item.podcast.id);

        this.DonwloadManager
            .ws
            .subscribe(webSockedUrl, (message) => {
                let itemFromWS = JSON.parse(message.body);
                if (itemFromWS.id == this.item.id) {
                    _.assign(this.item, itemFromWS);
                }
            }, $scope);

    }

    stopDownload(item) {
        this.DonwloadManager.ws.stop(item);
    }

    toggleDownload(item) {
        this.DonwloadManager.ws.toggle(item);
    }

    remove(item) {
        return item.remove()
            .then(() => {
            this.playlistService.remove(item);
            this.$location.path('/podcast/'.concat(this.item.podcast.id));
        });
    }

    reset(item) {
        return item.reset()
            .then((itemReseted) => {
                _.assign(this.item, itemReseted);
                this.playlistService.remove(item);
            });
    }

    toggleInPlaylist() {
        this.playlistService.addOrRemove(this.item);
    }

    isInPlaylist() {
        return this.playlistService.contains(this.item);
    }
}

angular.module('ps.item.details', [
    'ps.dataService.donwloadManager',
    'ps.player'
]).config(function($routeProvider, commonKey) {
    $routeProvider.
        when('/podcasts/:podcastId/item/:itemId', {
            templateUrl: 'html/item-detail.html',
            controller: 'ItemDetailCtrl',
            controllerAs: 'idc',
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
    .controller('ItemDetailCtrl', ItemDetailCtrl);