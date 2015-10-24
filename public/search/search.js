
import angular from 'angular';
import SearchCtrl from './search.controller';
import SearchItemCache from './searchitemcache.service';

import NgStorage from 'ngstorage';

import ngTagsInput from 'config/ngTagsInput';
import AppRouteConfig from 'config/route.config';

import DownloadManager from 'common/service/data/downloadManager';
import ItemService from 'common/service/data/itemService';
import TagService from 'common/service/data/tagService';
import PlaylistService from 'common/service/playlistService';

export default angular.module('ps.search', [
    ngTagsInput.name,
    NgStorage.name,
    AppRouteConfig.name,
    DownloadManager.name,
    ItemService.name,
    TagService.name,
    PlaylistService.name
])
    .config(SearchCtrl.routeConfig)
    .constant('DefaultItemSearchParameters', {
        page : 0,
        size : 12,
        term : undefined,
        tags : undefined,
        direction : 'DESC',
        properties : 'pubdate',
        downloaded : "true"
    })
    .controller('ItemsSearchCtrl', SearchCtrl)
    .service("SearchItemCache", SearchItemCache);