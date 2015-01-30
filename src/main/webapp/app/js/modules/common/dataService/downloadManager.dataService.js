angular.module('ps.dataService.donwloadManager', [
    'restangular'
])
    .factory('DonwloadManager', function(Restangular) {
    'use strict';
        
        var baseTask = Restangular.one("task"),
            baseDownloadManager = baseTask.one('downloadManager');
        
    return {
        download: function (item) {
            return Restangular.one("item").customGET(item.id + "/addtoqueue");
        },
        stopDownload: function (item) {
            return baseDownloadManager.customPOST(item.id, "stopDownload");
        },
        toggleDownload: function (item) {
            return baseDownloadManager.customPOST(item.id, "toogleDownload");
        },
        stopAllDownload: function () {
            return baseDownloadManager.customGET("stopAllDownload");
        },
        pauseAllDownload: function () {
            return baseDownloadManager.customGET("pauseAllDownload");
        },
        restartAllCurrentDownload: function () {
            return baseDownloadManager.customGET("restartAllCurrentDownload");
        },
        removeFromQueue: function (item) {
            return baseDownloadManager.customDELETE("queue/" + item.id);
        },
        updateNumberOfSimDl: function (number) {
            return baseDownloadManager.customPOST(number, "limit");
        },
        dontDonwload: function (item) {
            return baseDownloadManager.customDELETE("queue/" + item.id + "/andstop");
        },
        getDownloading : function() {
            return baseTask.all("downloadManager/downloading").getList();
        },
        getNumberOfSimDl : function() {
            return baseDownloadManager.one("limit").get();
        },
        moveInWaitingList : function (item, position) {
            baseDownloadManager.customPOST({id : item.id, position : position } , 'move');
        }
    };
});