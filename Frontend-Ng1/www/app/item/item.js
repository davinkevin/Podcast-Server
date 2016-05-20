/**
*  Created by kevin on 01/11/14 for Podcast Server
*/
import {Module} from '../decorators';
import ItemDetailsModule from './details/item.details';
import ItemPlayer from './player/item.player';

@Module({
    name : 'ps.item',
    modules : [ItemDetailsModule, ItemPlayer]
})
export default class Item{}