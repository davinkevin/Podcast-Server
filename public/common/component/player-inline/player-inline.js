import angular from 'angular';
import AppVideogularConfig from 'config/videogular';
import Truncate from 'config/truncate';
import PlaylistService from 'common/service/playlistService';

import PlayerInlineComponent from './player-inline.component.js'

export default angular.module('ps.common.component.players-inline', [
    AppVideogularConfig.name,
    Truncate.name,
    PlaylistService.name
])
    .directive('playerInline', PlayerInlineComponent.component)
    .controller('PlayerInlineController', PlayerInlineComponent);
