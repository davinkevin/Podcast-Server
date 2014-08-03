var podcastServices = angular.module('podcast.services', [/*'ngResource'*/]);

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

    downloadManager.dontDonwload = function(item) {
        Restangular.one("task").customDELETE("downloadManager/queue/" + item.id + "/andstop");
    };

    return downloadManager;
});