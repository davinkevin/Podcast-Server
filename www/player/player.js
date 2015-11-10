import angular from 'angular';
import AppVideogularConfig from '../config/videogular';
import AppRouteConfig from '../config/route.config';
import DeviceDetectionService from '../common/component/device-detection/device-detection';
import PlaylistService from '../common/service/playlistService';

import PlayerController from './player.controller';
import './player.css!';

export default angular.module('ps.player', [
    AppVideogularConfig.name,
    AppRouteConfig.name,
    DeviceDetectionService.name,
    PlaylistService.name
])
    .config(PlayerController.routeConfig)
    .controller(PlayerController.name, PlayerController);