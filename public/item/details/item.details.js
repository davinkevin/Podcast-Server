
import angular from 'angular';
import DownloadManager from 'common/service/data/downloadManager';
import PlaylistService from 'common/service/playlistService';
import HtmlFilters from 'common/filter/html2plainText';
import ItemDetailCtrl from './item.details.controller';

export default angular
    .module('ps.item.details', [
        DownloadManager.name,
        PlaylistService.name,
        HtmlFilters.name
    ])
    .config(ItemDetailCtrl.routeConfig)
    .controller('ItemDetailCtrl', ItemDetailCtrl);