

angular.module('ps.websocket', [
    'AngularStomp'
]).service('podcastWebSocket', function (ngstomp, $log, $q) {
    'use strict';
    var self = this,
        wsClient = ngstomp("/ws", SockJS),
        deferred = $q.defer(),
        promiseResult = deferred.promise;


    self.connect = function(){
        wsClient.connect("user", "password", function () {
           self.isConnected = true;
           $log.info("Connection to the WebSockets");
           deferred.resolve();
        });
        return promiseResult;
    };

    self.subscribe = function(url, callback, scope) {
        promiseResult.then(function() {
            wsClient.subscribe(url, callback);
            if (scope !== undefined) {
                scope.$on('$destroy', function () {
                    self.unsubscribe(url);
                });
            }
        });
        return self;
    };

    self.unsubscribe = function(queue, callback) {
        promiseResult.then(function() {
            wsClient.unsubscribe(queue, callback);
        });
        return self;
    };

    self.connect();
    return self;
});