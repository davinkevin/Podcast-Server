angular.module('ps.dataService.donwloadManager', [
    'restangular'
])
    .factory('DonwloadManager', function(Restangular) {
    'use strict';
    return {
        download: function (item) {
            return Restangular.one("item").customGET(item.id + "/addtoqueue");
        },
        stopDownload: function (item) {
            return Restangular.one("task").customPOST(item.id, "downloadManager/stopDownload");
        },
        toggleDownload: function (item) {
            return Restangular.one("task").customPOST(item.id, "downloadManager/toogleDownload");
        },
        stopAllDownload: function () {
            return Restangular.one("task").customGET("downloadManager/stopAllDownload");
        },
        pauseAllDownload: function () {
            return Restangular.one("task").customGET("downloadManager/pauseAllDownload");
        },
        restartAllCurrentDownload: function () {
            return Restangular.one("task").customGET("downloadManager/restartAllCurrentDownload");
        },
        removeFromQueue: function (item) {
            return Restangular.one("task").customDELETE("downloadManager/queue/" + item.id);
        },
        updateNumberOfSimDl: function (number) {
            return Restangular.one("task").customPOST(number, "downloadManager/limit");
        },
        dontDonwload: function (item) {
            return Restangular.one("task").customDELETE("downloadManager/queue/" + item.id + "/andstop");
        }
    };
});