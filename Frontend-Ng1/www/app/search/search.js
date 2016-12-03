/**
 * Created by kevin on 24/10/2015 for PodcastServer
 */

import angular from 'angular';
import {Component, Module, Constant, Service} from '../decorators';
import {TitleService} from '../common/service/title.service';
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
        ItemMenu, NgTagsInput, NgStorage, AppRouteConfig, DownloadManager,
        ItemService, TagService, PlaylistService, TitleService
    ]
})
@Component({
    selector : 'search',
    as : 'isc',
    template : template,

    path : '/items',
    reloadOnSearch : false,
    resolve : { page : (itemService, SearchItemCache) => {"ngInject"; return itemService.search(SearchItemCache.parameters);}}
})
@Constant({
    name : 'DefaultItemSearchParameters',
    value : { page: 0, size: 12, q: undefined, tags: [], sort: [{ direction : 'DESC', property : 'pubDate'}], downloaded : "true"}
})
export default class ItemSearchCtrl {

    totalItems = Number.MAX_VALUE;
    maxSize = 10;

    constructor($scope, SearchItemCache, $location, itemService, tagService, DonwloadManager, playlistService, hotkeys, TitleService) {
        "ngInject";
        this.$scope = $scope;
        this.SearchItemCache = SearchItemCache;
        this.$location = $location;
        this.itemService = itemService;
        this.tagService = tagService;
        this.DownloadManager = DonwloadManager;
        this.playlistService = playlistService;
        this.hotkeys = hotkeys;
        this.TitleService = TitleService;
    }

    $onInit() {
        this.currentPage = this.SearchItemCache.page + 1;
        this.searchParameters = this.SearchItemCache.parameters;
        this.TitleService.title = 'Search';

        this.hotkeys
            .bindTo(this.$scope)
            .add({ combo: 'right', description: 'Next page', callback: () => this.swipePage(1) })
            .add({ combo: 'left', description: 'Previous page', callback: () => this.swipePage(-1) });

        this.attachResponse(this.page);

        this.elemInPageSub = this.DownloadManager
            .download$
            .filter(item => this.items.some(elem => elem.id === item.id))
            .subscribe(item => this.$scope.$evalAsync(() => Object.assign(this.items.find(elem => elem.id === item.id), item)));

        this.isNewDownloadedSub = this.DownloadManager
            .download$
            .filter(item => item.isDownloaded)
            .subscribe(() => this.$scope.$evalAsync(() => this.changePage()));

        this.isUpdatingSub = this.DownloadManager
            .updating$
            .filter(isUpdating => isUpdating === false)
            .subscribe(() => this.$scope.$evalAsync(() => this.changePage()));

        this.$scope.$on('$routeUpdate', () => {
            if (this.currentPage !== this.$location.search().page) {
                this.currentPage = this.$location.search().page || 1;
                this.changePage();
            }
        });                   
    }

    $onDestroy() {
        this.elemInPageSub.dispose();
        this.isNewDownloadedSub.dispose();
        this.isUpdatingSub.dispose();
    }

    updateItemFromWS(item) {
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

    attachResponse({content, totalPages, totalElements, number}) {
        this.items = content;
        this.totalPages = totalPages;
        this.totalItems = totalElements;
        this.SearchItemCache.page = number;
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

    updatePageWhenUpdateDone(isInUpdate) {
        if(isInUpdate === false) { this.changePage(); }
    }

    play(item) {
        this.itemService.play(item);
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