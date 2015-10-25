import angular from 'angular';
import AppLoadingBar from './loading.config';
import uiBootstrap from 'angular-bootstrap';
import ngFileUpload from 'ng-file-upload';
import PlayerInlineModule from 'common/component/player-inline/player-inline';
import 'angular-touch';
import 'angular-animate';
import 'angular-truncate';


console.log(ngFileUpload);

export default angular.module('ps.config', [
    AppLoadingBar.name,
    'ngTouch',
    'ngAnimate',
    uiBootstrap,
    'truncate',
    ngFileUpload,
    'ps.common.component.players-inline',
    'ps.common.component.navbar',
    'ps.common.component.authorize-notification',
    'ps.common.component.device-detection',
    'ps.common.component.updating'
]);