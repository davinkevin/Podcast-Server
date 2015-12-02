/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, View, Module} from '../../../decorators';
import AngularNotification from '../../../common/modules/angularNotification';
import template from './authorize-notification.html!text';

@Module({
    name : 'ps.common.component.authorize-notification',
    modules : [ AngularNotification ]
})
@Component({
    selector : 'authorize-notification',
    replace : true,
    as : 'an'
})
@View({
    template : template
})
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
}

