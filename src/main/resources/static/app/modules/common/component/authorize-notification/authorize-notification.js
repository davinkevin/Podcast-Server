
class authorizeNotificationDirective {
    constructor() {
        this.replace = true;
        this.restrict = 'E';
        this.scope = true;
        this.templateUrl = 'common/component/authorize-notification/authorize-notification.html';
        this.controllerAs = 'an';
        this.controller = 'authorizeNotificationController';
    }
}

class authorizeNotificationController {
    constructor($window, $notification) {
        this.$window = $window;
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

angular.module('ps.common.component.authorize-notification', ['notification'])
    .directive('authorizeNotification', () => new authorizeNotificationDirective())
    .controller('authorizeNotificationController', authorizeNotificationController);
