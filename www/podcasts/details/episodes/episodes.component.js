/**
    * Created by kevin on 25/10/2015 for PodcastServer
    */

import template from './episodes.html!text';

export default class PodcastItemsListComponent {

    constructor($scope, DonwloadManager, PodcastItemPerPage, itemService, playlistService ) {
        "ngInject";
        /* DI */
        this.$scope = $scope;
        this.DownloadManager = DonwloadManager;
        this.itemService = itemService;
        this.playlistService = playlistService;

        this.currentPage = 1;
        this.itemPerPage = PodcastItemPerPage;
        this.loadPage();

        this.$scope.$on("podcastItems:refresh", () => {
            this.currentPage = 1;
            this.loadPage();
        });

        this.DownloadManager
            .ws
            .subscribe( "/topic/podcast/".concat(this.podcast.id),
            (message) => this.onMessageFromWS(message),
            $scope);
    }

    onMessageFromWS(message) {
        var item = JSON.parse(message.body);
        var elemToUpdate = _.find(this.podcast.items, { 'id': item.id });
        _.assign(elemToUpdate, item);
    }

    loadPage() {
        this.currentPage = (this.currentPage < 1) ? 1 : (this.currentPage > Math.ceil(this.totalItems / this.itemPerPage)) ? Math.ceil(this.totalItems / this.itemPerPage) : this.currentPage;
        return this.itemService
            .getItemForPodcastWithPagination(this.podcast, {size: this.itemPerPage, page : this.currentPage - 1, direction : 'DESC', properties : 'pubdate'})
            .then((itemsResponse) => {
                this.podcast.items = this.itemService.restangularizePodcastItem(this.podcast, itemsResponse.content);
                this.podcast.totalItems = itemsResponse.totalElements;
            });
    }

    remove(item) {
        item.remove()
            .then(() => this.podcast.items = _.reject(this.podcast.items, (elem) => {return (elem.id === item.id); }))
            .then(() => this.playlistService.remove(item))
            .then(() => this.loadPage());
    }


    reset(item) {
        return item.reset()
            .then((itemReseted) => {
                var itemInList = _.find(this.podcast.items, { 'id': itemReseted.id });
                _.assign(itemInList, itemReseted);
                return itemInList;
            })
            .then((itemToRemove) => this.playlistService.remove(itemToRemove));
    }

    addOrRemoveInPlaylist(item) {
        this.playlistService.addOrRemove(item);
    }

    isInPlaylist(item) {
        return this.playlistService.contains(item);
    }

    swipePage(val) {
        this.currentPage += val;
        this.loadPage();
    }

    stopDownload(item) {
        this.DownloadManager.ws.stop(item);
    }
    toggleDownload(item) {
        this.DownloadManager.ws.toggle(item);
    }

    static component() {
        return {
            restrict : 'E',
            template : template,
            scope : { podcast : '=' },
            controller : 'PodcastItemsListCtrl',
            controllerAs : 'pic',
            bindToController : true
        };
    }
}
