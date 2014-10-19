'use strict';

angular.module('podcast.websocket', [
    'AngularStomp'
]).service('podcastWebSocket', function (ngstomp, $log, $q) {

    var self = this,
        wsClient = ngstomp("/ws", SockJS),
        deferred = $q.defer(),
        promiseResult = deferred.promise;


    this.connect = function(){
        wsClient.connect("user", "password", function () {
           self.isConnected = true;
           $log.info("Connection to the WebSockets");
           deferred.resolve();
        });
        return promiseResult;
    };

    this.subscribe = function(url, callback) {
        promiseResult.then(function() {
            wsClient.subscribe(url, callback);
        });
        return self;
    };

    this.unsubscribe = function(queue, callback) {
        promiseResult.then(function() {
            wsClient.unsubscribe(queue, callback);
        });
        return self;
    };

    this.connect();
    return this;
});