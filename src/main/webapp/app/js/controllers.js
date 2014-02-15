var podcastControllers = angular.module('podcastControllers', []);

podcastControllers.controller('ItemsListCtrl', function ($scope, $http, $routeParams, Restangular) {
    /*Restangular.one("item/pagination").get({size: 12, page :0, direction : 'DESC', properties : 'pubdate'}).then(function(itemsResponse) {
        $scope.items = itemsResponse.content;

        $scope.totalItems = itemsResponse.totalElements;
        $scope.currentPage = itemsResponse.number+1;
        $scope.itemsperpage = 12;
        $scope.maxSize = 10;
    });
    */

    //$scope.selectPage = function (pageNo) {
    $scope.$watch('currentPage', function(newPage){
        Restangular.one("item/pagination").get({size: 12, page :newPage-1, direction : 'DESC', properties : 'pubdate'}).then(function(itemsResponse) {
            $scope.items = itemsResponse.content;

            $scope.totalItems = itemsResponse.totalElements;
            $scope.maxSize = 10;
        });
    });
    $scope.currentPage = 1;

    $scope.download = function(item) {
        $http.get('/api/item/' + item.id + "/addtoqueue");
    }

});

podcastControllers.controller('ItemDetailCtrl', function ($scope, $routeParams, $http, Item) {
    $scope.item = Item.get({itemId: $routeParams.itemId});

    $scope.download = function() {
        $http.get('/api/item/' + $routeParams.itemId + "/addtoqueue");
    }
});

podcastControllers.controller('PodcastsListCtrl', function ($scope, $http, Restangular) {
    Restangular.all("podcast").getList().then(function(podcasts) {
        $scope.podcasts = podcasts;
    });
});

podcastControllers.controller('PodcastDetailCtrl', function ($scope, $http, $routeParams, Restangular, ngstomp) {
    //$scope.podcast = Podcast.get({podcastId: $routeParams.podcastId});
    Restangular.one("podcast", $routeParams.podcastId).get().then(function(data) {
        $scope.podcast = data;

        $scope.wsClient = ngstomp("/download", SockJS, function(str) { console.log(str)});
        $scope.wsClient.connect("user", "password", function(){
            $scope.wsClient.subscribe("/topic/podcast/" + $scope.podcast.id, function(message) {
                var item = JSON.parse(message.body);
                var elemToUpdate = _.find($scope.podcast.items, { 'id': item.id });
                _.assign(elemToUpdate, item);
                console.log(item);
            });
        });

    });


    $scope.download = function(item) {
        $http.get('/api/item/' + item.id + "/addtoqueue");
    }
    $scope.remove = function(item) {
        Restangular.one("item", item.id).remove().then(function() {
            $scope.podcast.items = _.reject($scope.podcast.items, function(elem) {
                return (elem.id == item.id);
            });
        });
    }
    $scope.refresh = function() {
        $http.post("/api/task/updateManager/updatePodcast/force", $scope.podcast.id).success(function() {
            $scope.podcast.get().then(function(podcast) {
                $scope.podcast.items = podcast.items;
            });
        });
    }
    $scope.stopDownload = function(item) {
        $http.post("/api/task/downloadManager/stopDownload", item.id);
    }
    $scope.toggleDownload = function(item) {
        $http.post("/api/task/downloadManager/toogleDownload", item.id);
    }



    $scope.save = function() {
        var podcastToUpdate = _.cloneDeep($scope.podcast);
        podcastToUpdate.items = null;
        $scope.podcast.patch(podcastToUpdate);
    }
});

podcastControllers.controller('DownloadCtrl', function ($scope, $http, $routeParams, Restangular, ngstomp) {
    $scope.items = Restangular.all("task/downloadManager/downloading").getList().$object;
    $scope.waitingitems = Restangular.all("task/downloadManager/queue").getList().$object;
    Restangular.one("task/downloadManager/limit").get().then(function(data) {
        $scope.numberOfSimDl = parseInt(data);
    });

    $scope.updateNumberOfSimDl = function() {
        $http.post("/api/task/downloadManager/limit",$scope.numberOfSimDl);
    }

    /** Spécifique aux éléments de la liste : **/
    $scope.stopDownload = function(item) {
        $http.post("/api/task/downloadManager/stopDownload", item.id);
    }
    $scope.toggleDownload = function(item) {
        $http.post("/api/task/downloadManager/toogleDownload", item.id);
    }

    /** Global **/
    $scope.stopAllDownload = function() {
            $http.get("/api/task/downloadManager/stopAllDownload");
    }
    $scope.pauseAllDownload = function() {
            $http.get("/api/task/downloadManager/pauseAllDownload");
    }
    $scope.restartAllCurrentDownload = function() {
            $http.get("/api/task/downloadManager/restartAllCurrentDownload");
    }
    $scope.removeFromQueue = function(item) {
        $http.delete("/api/task/downloadManager/queue/" + item.id);
    }

    /*
    var refreshIntervalId = $interval(function() {
        Restangular.all("task/downloadManager/downloading").getList().then(function(items) {
            $scope.items = items;
        });
        Restangular.all("task/downloadManager/queue").getList().then(function(waitingitems) {
            $scope.waitingitems = waitingitems;
        });
    }, 3000);

     $scope.$on('$destroy', function () {
        $interval.cancel(refreshIntervalId);
    });
    */
    $scope.wsClient = ngstomp('/download', SockJS);
    $scope.wsClient.connect("user", "password", function(){
        $scope.wsClient.subscribe("/topic/download", function(message) {
            var item = JSON.parse(message.body);

            var elemToUpdate = _.find($scope.items, { 'id': item.id });

            switch (item.status) {
                case 'Started' :
                case 'Paused' :
                    if (elemToUpdate)
                        _.assign(elemToUpdate, item);
                    else
                        $scope.items.push(item);

                    break;
                case 'Stopped' :
                case 'Finish' :
                    if (elemToUpdate)
                        _.remove($scope.items, function(item) { return item.id == elemToUpdate.id})
                    break;
            }

        });
    });

});

podcastControllers.controller('PodcastAddCtrl', function ($scope, Restangular) {
    var podcasts = Restangular.all("podcast");

    $scope.podcast = {};
    $scope.podcast.hasToBeDeleted = true;


    $scope.save = function() {
        podcasts.post($scope.podcast);
    }
});