import angular from 'angular';
import AppVideogularConfig from 'config/videogular';
import AppRouteConfig from 'config/route.config';
import DeviceDetectionService from 'common/component/device-detection/device-detection';
import ItemPlayerController from './item.player.controller';

export default angular.module('ps.item.player', [
    AppRouteConfig.name,
    AppVideogularConfig.name,
    DeviceDetectionService.name
])
    .config(ItemPlayerController.routeConfig)
    .controller('ItemPlayerController', ItemPlayerController);