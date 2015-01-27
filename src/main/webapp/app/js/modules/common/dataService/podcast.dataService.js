/**
 * Created by kevin on 02/11/14.
 */

angular.module('ps.dataService.podcast', [
    'restangular'
]).factory('podcastService', function (Restangular) {
    'use strict';
    var route = 'podcast';

    return {
        findById    :   findById,
        findAll     :   findAll,
        save        :   save,
        getNewPodcast : getNewPodcast,
        patch       :   patch,
        deletePodcast : deletePodcast
    };

    function findById(podcastId) {
        return Restangular.one(route, podcastId).get();
    }

    function findAll() {
        return Restangular.all(route).getList();
    }
    
    function save(podcast) {
        return podcast.save();
    }
    
    function getNewPodcast() {
        return Restangular.one(route);
    }
    
    function patch(item) {
        return item.patch();
    }
    
    function deletePodcast(item) {
        return item.remove();
    }
});