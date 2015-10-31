import angular from 'angular';
import AppLoadingBar from './loading.config';
import uiBootstrap from 'angular-bootstrap';
import ngFileUpload from 'ng-file-upload';
import PlayerInlineModule from 'common/component/player-inline/player-inline';
import NavbarModule from 'common/component/navbar/navbar';
import AuthorizeNotificationModule from 'common/component/authorize-notification/authorize-notification';
import UpdatingModule from 'common/component/updating/updating';
import 'angular-touch';
import 'angular-animate';
import 'angular-truncate';
import 'common/mixins.js';
import './bootstrap/bootstrap';
import './styles/styles';

export default angular.module('ps.config', [
    'ngTouch',
    'ngAnimate',
    'truncate',
    uiBootstrap,
    ngFileUpload,
    AppLoadingBar.name,
    PlayerInlineModule.name,
    NavbarModule.name,
    AuthorizeNotificationModule.name,
    UpdatingModule.name
]);