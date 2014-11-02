/**
 * Created by kevin on 02/11/14.
 */

angular.module('ps.dataService.podcast', [
    'restangular'
]).factory('podcastService', function (Restangular) {
    'use strict';

    return {
        findById    :   findById,
        findAll     :   findAll
    };

    function findById(podcastId) {
        return Restangular.one('podcast', podcastId).get();
    }

    function findAll() {
        return Restangular.all('podcast').getList();
    }
});