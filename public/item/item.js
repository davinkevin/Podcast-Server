/**
*  Created by kevin on 01/11/14 for Podcast Server
*/

import ItemDetailsModule from './details/item.details';

export default angular.module('ps.item', [
    'ps.item.details',
    'ps.item.player'
]);