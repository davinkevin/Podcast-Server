/**
    * Created by kevin on 25/10/2015 for PodcastServer
    */

import template from './authorize-notification.html!text';

export default class AuthorizeNotificationComponent {
    constructor($window, $notification) {
        "ngInject";
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
    
    static component() {
        return {
            replace : true,
            restrict : 'E',
            scope : true,
            template : template,
            controllerAs : 'an',
            controller : 'authorizeNotificationController'
        }
    }
}

