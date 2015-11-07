import angular from 'angular';

import NgStorage from 'ngstorage';

import NgTagsInput from '../config/ngTagsInput';
import AppRouteConfig from '../config/route.config';
import DownloadManager from '../common/service/data/downloadManager';
import ItemService from '../common/service/data/itemService';
import TagService from '../common/service/data/tagService';
import PlaylistService from '../common/service/playlistService';

import ItemSearchCtrl from './search.controller';
import SearchItemCache from './searchitemcache.service';

export default angular.module('ps.search', [
    NgTagsInput.name,
    NgStorage.name,
    AppRouteConfig.name,
    DownloadManager.name,
    ItemService.name,
    TagService.name,
    PlaylistService.name
])
    .config(ItemSearchCtrl.routeConfig)
    .constant('DefaultItemSearchParameters', {
        page : 0,
        size : 12,
        term : undefined,
        tags : undefined,
        direction : 'DESC',
        properties : 'pubdate',
        downloaded : "true"
    })
    .controller(ItemSearchCtrl.name, ItemSearchCtrl)
    .service("SearchItemCache", SearchItemCache);