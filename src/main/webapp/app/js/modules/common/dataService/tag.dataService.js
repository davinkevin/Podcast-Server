/**
 * Created by kevin on 01/11/14.
 */

angular.module('ps.dataService.tag', [
    'restangular'
]).factory('tagService', function (Restangular) {
    'use strict';
    var baseAll = Restangular.all('tag');

    return {
        getAll : getAll,
        search : search
    };

    function getAll() {
        return baseAll.get();
    }

    function search(query) {
        return baseAll.post(null, {name : query});
    }
});