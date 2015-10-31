/**
 * Created by kevin on 24/10/2015 for PodcastServer
 */

import template from './search.html!text';
import style from './search.css!';

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
        this.currentPage = this.SearchItemCache.page()+1;
        this.searchParameters = this.SearchItemCache.getParameters();

        //** WebSocket Subscription **//
        this.DownloadManager
            .ws
            .subscribe("/topic/download", (message) => this.updateItemFromWS(message), $scope);

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

        var elemToUpdate = _.find(this.items, { 'id': item.id });
        if (elemToUpdate)
            _.assign(elemToUpdate, item);
    }

    changePage() {
        this.SearchItemCache.page(this.calculatePage());
        return this.itemService
            .search(this.SearchItemCache.getParameters())
            .then((itemsResponse) => this.attachResponse(itemsResponse));
    }

    attachResponse(itemsResponse) {
        this.items = itemsResponse.content;
        this.totalPages = itemsResponse.totalPages;
        this.totalItems = itemsResponse.totalElements;
        this.currentPage = this.SearchItemCache.page(itemsResponse.number)+1;
        this.$location.search("page", this.currentPage);
    }

    swipePage(val) {
        this.currentPage = this.SearchItemCache.page() + val + 1;
        return this.changePage();
    }

    //** Item Operation **//
    remove(item) {
        return item.remove()
            .then(() => this.playlistService.remove(item))
            .then(() => this.changePage());
    }

    reset(item) {
        return item.reset()
            .then((itemReseted) => {
                var itemInList = _.find(this.items, { 'id': itemReseted.id });
                _.assign(itemInList, itemReseted);
                return itemInList;
            })
            .then((itemInList) => this.playlistService.remove(itemInList));
    }

    stopDownload(item) {
        this.DownloadManager.ws.stop(item);
    }

    toggleDownload(item){
        return this.DownloadManager.ws.toggle(item);
    }

    loadTags(query){
        return this.tagService.search(query);
    }

    //** Playlist Manager **//
    addOrRemove(item) {
        return this.playlistService.addOrRemove(item);
    }

    isInPlaylist(item) {
        return this.playlistService.contains(item);
    }

    calculatePage() {
        if (this.currentPage <= 1) {
            return 0;
        } else if (this.currentPage > Math.ceil(this.totalItems / this.SearchItemCache.size())) {
            return Math.ceil(this.totalItems / this.SearchItemCache.size()) - 1;
        } else {
            return this.currentPage - 1;
        }
    }

    resetSearch() {
        this.currentPage = 1;
        this.SearchItemCache.updateSearchParam(this.searchParameters);
        return this.changePage();
    }

    static routeConfig($routeProvider, commonKey) {
        "ngInject";
        $routeProvider.
            when('/items', {
                template: template,
                controller: 'ItemsSearchCtrl',
                controllerAs: 'isc',
                reloadOnSearch: false,
                hotkeys: [
                    ['right', 'Next page', 'isc.swipePage(1)'],
                    ['left', 'Previous page', 'isc.swipePage(-1)']
                ].concat(commonKey),
                resolve : { items : (itemService, SearchItemCache) => itemService.search(SearchItemCache.getParameters()) }
            });
    }
}