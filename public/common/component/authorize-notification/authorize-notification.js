import angular from 'angular';
import AngularNotification from 'config/angularNotification';
import AuthoriszeNotificationComponent from './authorize-notification.component';

export default angular.module('ps.common.component.authorize-notification', [
    AngularNotification.name
])
    .directive('authorizeNotification', AuthoriszeNotificationComponent.component)
    .controller('authorizeNotificationController', AuthoriszeNotificationComponent);
