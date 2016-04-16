/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../../../decorators';
import AngularNotification from '../../../common/modules/angularNotification';
import template from './authorize-notification.html!text';

@Module({
    name : 'ps.common.component.authorize-notification',
    modules : [ AngularNotification ]
})
@Component({
    selector : 'authorize-notification',
    as : 'an',
    template : template
})
export default class AuthorizeNotificationComponent {
    
    constructor($window, $notification) {
        "ngInject";
        this.$window = $window;
        this.$notification = $notification;
    }
    
    $onInit() { this.state = this.hasToBeShown(); }

    manuallyactivate() { this.$notification.requestPermission().then(() => this.state = this.hasToBeShown()); }

    hasToBeShown() { return (('Notification' in this.$window) && this.$window.Notification.permission === 'default'); }
}

