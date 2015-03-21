
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
    constructor($window, $notification, $q) {
        this.$window = $window;
        this.$q = $q;
        this.$notification = $notification;
        this.state = this.hasToBeShown();
    }

    manuallyactivate() {
        this.$notification
            .requestPermission()
            .then(() => { this.state = this.hasToBeShown();});
    }

    hasToBeShown() {
        return (('Notification' in this.$window) && this.$window.Notification.permission != 'granted');
    }
}

angular.module('authorize-notification', ['notification'])
    .directive('authorizeNotification', () => new authorizeNotificationDirective())
    .controller('authorizeNotificationController', authorizeNotificationController);
