angular.module('authorize-notification', [
    'notification'
]).directive('authorizeNotification', function() {
    return {
        replace : true,
        restrcit : 'E',
        templateUrl : 'html/authorize-notification.html',
        scope : true,
        controllerAs : 'an',
        controller : 'authorizeNotificationController'
    };
}).controller('authorizeNotificationController', function($window, Notification, $rootScope){
    var vm = this;

    //** https://code.google.com/p/chromium/issues/detail?id=274284 **/
    // Issue fixed in the M37 of Chrome :
    vm.state = hasToBeShown();
    vm.manuallyactivate = function() {
        Notification.requestPermission(function() {
            vm.state = hasToBeShown();
            $rootScope.$digest();
        });
    };

    function hasToBeShown() {
        return (('Notification' in $window) && $window.Notification.permission != 'granted');
    }
});
