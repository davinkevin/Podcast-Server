angular.module('ps.dataService.updateService', [
    'restangular'
])
    .factory('UpdateService', function(Restangular) {
        'use strict';
        
        return {
            forceUpdatePodcast : forceUpdatePodcast
        };

        function forceUpdatePodcast(idPodcast) {
            return Restangular.one("task").customPOST(idPodcast, "updateManager/updatePodcast/force");
        }
    });