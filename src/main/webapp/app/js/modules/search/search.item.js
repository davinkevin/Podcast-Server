class ItemSearchCtrl {

    constructor($scope, $cacheFactory, $location, itemService, tagService, DonwloadManager, ItemPerPage, playlistService, items) {
        /* DI */
        this.$location = $location;
        this.itemService = itemService;
        this.tagService = tagService;
        this.DownloadManager = DonwloadManager;
        this.playlistService = playlistService;

        /* Constructor Init */
        this.cache = $cacheFactory.get('paginationCache') || $cacheFactory('paginationCache');

        this.totalItems = Number.MAX_VALUE;
        this.maxSize = 10;
        this.currentPage = this.cache.get("search:currentPage") || 1;

        this.searchParameters = {
            page : this.currentPage,
            size : ItemPerPage,
            term : this.cache.get("search:currentWord") || undefined,
            searchTags : this.cache.get("search:currentTags") || undefined,
            direction : this.cache.get("search:direction") || undefined,
            properties : this.cache.get("search:properties") || undefined
        };

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
        this.searchParameters.page = this.calculatePage();
        return this.itemService.search(this.searchParameters)
            .then((itemsResponse) => this.attachResponse(itemsResponse));
    }

    attachResponse(itemsResponse) {
        this.items = itemsResponse.content;
        this.totalPages = itemsResponse.totalPages;
        this.totalItems = itemsResponse.totalElements;

        this.cache.put('search:currentPage', this.currentPage);
        this.cache.put('search:currentWord', this.term);
        this.cache.put('search:currentTags', this.searchTags);
        this.cache.put("search:direction", this.direction);
        this.cache.put("search:properties", this.properties);

        this.$location.search("page", this.currentPage);
    }

    swipePage(val) {
        this.currentPage += val;
        return this.changePage();
    };

    //** Item Operation **//
    remove(item) {
        return item.remove()
            .then(() => playlistService.remove(item))
            .then(() => this.changePage());
    }

    reset(item) {
        return item.reset()
            .then((itemReseted) => {
                var itemInList = _.find(this.items, { 'id': itemReseted.id });
                _.assign(itemInList, itemReseted);
                return itemInList
            })
            .then((itemInList) => this.playlistService.remove(itemInList));
    };

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
    };

    isInPlaylist(item) {
        return this.playlistService.contains(item);
    };

    calculatePage() {
        return ((this.currentPage <= 1)
                ? 1
                : (this.currentPage > Math.ceil(this.totalItems / this.searchParameters.size))
                    ? Math.ceil(this.totalItems / this.searchParameters.size)
                    : this.currentPage
            ) - 1;
    }
}

angular.module('ps.search.item', [
    'ps.dataService.donwloadManager',
    'ps.dataService.item',
    'ps.dataService.tag',
    'ps.player',
    'ps.config.route',
    'ngTagsInput'
])
    .config(($routeProvider, commonKey) => {
        $routeProvider.
            when('/items', {
                templateUrl: 'html/items-search.html',
                controller: 'ItemsSearchCtrl',
                controllerAs: 'isc',
                reloadOnSearch: false,
                hotkeys: [
                    ['right', 'Next page', 'isc.currentPage = isc.currentPage+1; isc.changePage();'],
                    ['left', 'Previous page', 'isc.currentPage = isc.currentPage-1; isc.changePage();']
                ].concat(commonKey),
                resolve : {
                    items : (itemService, ItemPerPage, $location, $cacheFactory) => {
                        let parameters = { size : ItemPerPage };
                        parameters.page = ($cacheFactory.get('paginationCache') == undefined)
                                ? 0
                                : $cacheFactory.get('paginationCache').get("search:currentPage")-1 || 0;
                        return itemService.search(parameters);
                    }
                }
            });
    })
    .constant('ItemPerPage', 12)
    .controller('ItemsSearchCtrl', ItemSearchCtrl);