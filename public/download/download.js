import angular from 'angular';
import AngularNotification from 'config/angularNotification';
import AppRouteConfig from 'config/route.config';
import DownloadManager from 'common/service/data/downloadManager';
import DownloadCtrl from './download.controller';

export default angular.module('ps.download', [
    AngularNotification.name,
    AppRouteConfig.name,
    DownloadManager.name
])
    .config(DownloadCtrl.routeConfig)
    .controller('DownloadCtrl', DownloadCtrl);