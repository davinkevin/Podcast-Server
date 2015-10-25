import angular from 'angular';
import AppVideogularConfig from 'config/videogular';
import AppRouteConfig from 'config/route.config';
import DeviceDetectionService from 'common/component/device-detection/device-detection';
import PlaylistService from 'common/service/playlistService';

import PlayerCtrl from './player.controller';

export default angular.module('ps.player', [
    AppVideogularConfig.name,
    AppRouteConfig.name,
    DeviceDetectionService.name,
    PlaylistService.name
])
    .config(PlayerCtrl.routeConfig)
    .controller('PlayerController', PlayerCtrl);