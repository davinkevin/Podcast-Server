angular.module('podcast.controller')
    .controller('PodcastDetailCtrl', function ($scope, $routeParams, Restangular, ngstomp, localStorageService, DonwloadManager, $log, $location) {

        var idPodcast = $routeParams.podcastId,
            tags = Restangular.all("tag");

        $scope.tabs = [
            { title:'Episodes', templateUrl:'html/podcast-details-episodes.html', active : true },
            { title:'Modification', templateUrl:'html/podcast-details-edition.html', active : false }
        ];

        // LocalStorage de la valeur du podcast :
        $scope.$watchGroup(['podcast', 'podcast.items'], function(newval, oldval) {
            localStorageService.add("podcast/" + idPodcast, newval[0]);
        });

        $scope.podcast = localStorageService.get("podcast/" + idPodcast ) || {};

        $scope.refreshItems = function() {
            $scope.podcast.getList("items").then(function(items) {
                $scope.podcast.items = items;
            });
        };

        Restangular.one("podcast", $routeParams.podcastId).get().then(function(podcast) {
            podcast.items = $scope.podcast.items || [];
            $scope.podcast = podcast;


            $scope.wsClient = ngstomp("/download", SockJS);
            $scope.wsClient.connect("user", "password", function(){
                $scope.wsClient.subscribe("/topic/podcast/" + idPodcast, function(message) {
                    var item = JSON.parse(message.body);
                    var elemToUpdate = _.find($scope.podcast.items, { 'id': item.id });
                    _.assign(elemToUpdate, item);
                });
            });
            $scope.$on('$destroy', function () {
                $scope.wsClient.disconnect(function(){});
            });
        }).then($scope.refreshItems );


        $scope.remove = function(item) {
            item.remove().then(function() {
                $scope.podcast.items = _.reject($scope.podcast.items, function(elem) {
                    return (elem.id == item.id);
                });
            });
        };
        $scope.refresh = function() {
            Restangular.one("task").customPOST($scope.podcast.id, "updateManager/updatePodcast/force")
                .then($scope.refreshItems );
        };

        $scope.loadTags = function(query) {
            return tags.post(null, {name : query});
        };


        $scope.stopDownload = DonwloadManager.stopDownload;
        $scope.toggleDownload = DonwloadManager.toggleDownload;

        $scope.save = function() {
            var podcastToUpdate = _.cloneDeep($scope.podcast);
            podcastToUpdate.items = null;
            $scope.podcast.patch(podcastToUpdate).then(function(patchedPodcast){
                $log.debug(patchedPodcast);
                _.assign($scope.podcast, patchedPodcast);
            }).then($scope.refreshItems );
        };

        $scope.deletePodcast = function () {
            $scope.podcast.remove().then(function () {
                $location.path('/podcasts');
            });
        };

        $scope.reset = function (item) {
            return item.reset().then(function (itemReseted) {
                var itemInList = _.find($scope.podcast.items, { 'id': itemReseted.id });
                _.assign(itemInList, itemReseted);
            });
        };

    });