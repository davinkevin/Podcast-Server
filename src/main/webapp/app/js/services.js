var podcastServices = angular.module('podcastServices', ['ngResource']);

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