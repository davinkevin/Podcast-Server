
class authorizeNotificationDirective {
    constructor() {
        this.replace = true;
        this.restrict = 'E';
        this.scope = true;
        this.templateUrl = 'html/authorize-notification.html';
        this.controllerAs = 'an';
        this.controller = 'authorizeNotificationController';
    }
}

class authorizeNotificationController {
    constructor($window, Notification, $q) {
        this.$window = $window;
        this.$q = $q;
        this.Notification = Notification;
        this.state = this.hasToBeShown();
    }

    manuallyactivate() {
        this.notificationPromise()
            .then(() => { this.state = this.hasToBeShown();});
    }

    hasToBeShown() {
        return (('Notification' in this.$window) && this.$window.Notification.permission != 'granted');
    }

    notificationPromise() {
        let deferred = this.$q.defer();
        this.Notification.requestPermission(() => {
            deferred.resolve();
        });
        return deferred.promise;
    }
}

angular.module('authorize-notification', [ 'notification'])
    .directive('authorizeNotification', () => new authorizeNotificationDirective())
    .controller('authorizeNotificationController', authorizeNotificationController);
