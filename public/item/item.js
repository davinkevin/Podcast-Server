/**
*  Created by kevin on 01/11/14 for Podcast Server
*/
import angular from 'angular';
import ItemDetailsModule from './details/item.details';
import ItemPlayer from './player/item.player';

export default angular.module('ps.item', [
    ItemDetailsModule.name,
    ItemPlayer.name
]);