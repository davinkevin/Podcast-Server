/**
 * Created by kevin on 24/10/2015 for PodcastServer
 */

import angular from 'angular';
import {RouteConfig, View, HotKeys, Module, Constant, Service} from '../decorators';
import NgStorage from 'ngstorage';
import NgTagsInput from '../common/modules/ngTagsInput';
import AppRouteConfig from '../config/route';
import DownloadManager from '../common/service/data/downloadManager';
import ItemService from '../common/service/data/itemService';
import TagService from '../common/service/data/tagService';
import PlaylistService from '../common/service/playlistService';
import ItemMenu from '../common/component/item-menu/item-menu';
import template from './search.html!text';
import './search.css!';

@Module({
    name : 'ps.search',
    modules : [
        ItemMenu,
        NgTagsInput,
        NgStorage,
        AppRouteConfig,
        DownloadManager,
        ItemService,
        TagService,
        PlaylistService
    ]
})
@RouteConfig({
    path : '/items',
    as : 'isc',
    reloadOnSearch : false,
    resolve : {
        items : (itemService, SearchItemCache) => {"ngInject"; return itemService.search(SearchItemCache.parameters);}
    }
})
@HotKeys({
    hotKeys : [
        ['right', 'Next page', 'isc.swipePage(1)'],
        ['left', 'Previous page', 'isc.swipePage(-1)']
    ]
})
@Constant({
    name : 'DefaultItemSearchParameters',
    value : {
        page : 0,
        size : 12,
        term : undefined,
        tags : [],
        orders : [{ direction : 'DESC', property : 'pubDate'}],
        downloaded : "true"
    }
})
@View({
    template : template
})
export default class ItemSearchCtrl {

    constructor($scope, SearchItemCache, $location, itemService, tagService, DonwloadManager, playlistService, items) {
        "ngInject";
        /* DI */
        this.$location = $location;
        this.itemService = itemService;
        this.tagService = tagService;
        this.DownloadManager = DonwloadManager;
        this.playlistService = playlistService;
        this.SearchItemCache = SearchItemCache;

        /* Constructor Init */
        this.totalItems = Number.MAX_VALUE;
        this.maxSize = 10;
        this.currentPage = this.SearchItemCache.page + 1;
        this.searchParameters = this.SearchItemCache.parameters;

        //** WebSocket Subscription **//
        this.DownloadManager
            .ws
            .subscribe("/topic/download", (message) => this.updateItemFromWS(message), $scope)
            .subscribe('/topic/updating', (message) => this.updatePageWhenUpdateDone(message), {}, $scope);

        $scope.$on('$routeUpdate', () => {
            if (this.currentPage !== this.$location.search().page) {
                this.currentPage = this.$location.search().page || 1;
                this.changePage();
            }
        });

        /*this.changePage();*/
        this.attachResponse(items);
    }

    updateItemFromWS(wsMessage) {
        let item = JSON.parse(wsMessage.body);

        if (item.isDownloaded) {
            return this.changePage();
        }

        var elemToUpdate = this.items.find((elem) => elem.id === item.id);
        if (elemToUpdate)
            Object.assign(elemToUpdate, item);
    }

    changePage() {
        this.SearchItemCache.page = this.calculatePage();
        return this.itemService
            .search(this.SearchItemCache.parameters)
            .then((itemsResponse) => this.attachResponse(itemsResponse));
    }

    attachResponse(itemsResponse) {
        this.items = itemsResponse.content;
        this.totalPages = itemsResponse.totalPages;
        this.totalItems = itemsResponse.totalElements;
        this.SearchItemCache.page = itemsResponse.number;
        this.currentPage = this.SearchItemCache.page + 1;
        this.$location.search("page", this.currentPage);
    }

    swipePage(val) {
        this.currentPage = this.SearchItemCache.page + val + 1;
        return this.changePage();
    }

    loadTags(query){
        return this.tagService.search(query);
    }

    calculatePage() {
        if (this.currentPage <= 1) {
            return 0;
        } else if (this.currentPage > Math.ceil(this.totalItems / this.SearchItemCache.size)) {
            return Math.ceil(this.totalItems / this.SearchItemCache.size) - 1;
        } else {
            return this.currentPage - 1;
        }
    }

    resetSearch() {
        this.currentPage = 1;
        this.SearchItemCache.updateSearchParam(this.searchParameters);
        return this.changePage();
    }

    updatePageWhenUpdateDone(message) {
        if(JSON.parse(message.body) === false) {
            this.changePage();
        }
    }
}


@Module({
    inject : ItemSearchCtrl
})
@Service('SearchItemCache')
export class SearchItemCache {
    constructor(DefaultItemSearchParameters, $sessionStorage) {
        "ngInject";
        this.$sessionStorage = $sessionStorage;
        this.$sessionStorage.searchParameters = DefaultItemSearchParameters;
    }

    get parameters() {
        return this.$sessionStorage.searchParameters;
    }

    set page(page) {
        this.$sessionStorage.searchParameters.page = page;
    }

    get page() {
        return this.$sessionStorage.searchParameters.page;
    }

    set size(sizeNumber) {
        this.$sessionStorage.searchParameters.size = sizeNumber;
    }

    get size() {
        return this.$sessionStorage.searchParameters.size;
    }

    updateSearchParam(searchParam) {
        this.$sessionStorage.searchParameters = angular.extend({}, this.$sessionStorage.searchParameters, searchParam);
    }
}