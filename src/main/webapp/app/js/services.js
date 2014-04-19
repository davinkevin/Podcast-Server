var podcastServices = angular.module('podcastServices', ['ngResource']);

/*
podcastServices.factory('Podcast', ['$resource',
    function($resource){
        return $resource('/api/podcast/:podcastId', {}, {
            query: {method:'GET', params:{podcastId:''}, isArray:true}
        });
    }]);

podcastServices.factory('Item', ['$resource',
    function($resource){
        return $resource('/api/item/:itemId', {}, {
            query: {method:'GET', params:{itemId:''}, isArray:true}
        });
    }]);
*/

podcastServices.factory('DonwloadManager', function(Restangular) {
    var downloadManager = {};

    downloadManager.download = function(item) {
        Restangular.one("item").customGET(item.id + "/addtoqueue");
    };
    downloadManager.stopDownload = function(item) {
        Restangular.one("task").customPOST(item.id, "downloadManager/stopDownload");
    };
    downloadManager.toggleDownload = function(item) {
        Restangular.one("task").customPOST(item.id, "downloadManager/toogleDownload");
    };

    downloadManager.stopAllDownload = function() {
        Restangular.one("task").customGET("downloadManager/stopAllDownload");
    };
    downloadManager.pauseAllDownload = function() {
        Restangular.one("task").customGET("downloadManager/pauseAllDownload");
    };
    downloadManager.restartAllCurrentDownload = function() {
        Restangular.one("task").customGET("downloadManager/restartAllCurrentDownload");
    };
    downloadManager.removeFromQueue = function(item) {
        Restangular.one("task").customDELETE("downloadManager/queue/" + item.id);//.then($scope.refreshWaitingItems);
    };

    downloadManager.updateNumberOfSimDl = function(number) {
        Restangular.one("task").customPOST(number, "downloadManager/limit");
    };

    return downloadManager;
});