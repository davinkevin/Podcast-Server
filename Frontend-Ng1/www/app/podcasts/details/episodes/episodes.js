/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module, Constant} from '../../../decorators';
import HtmlFilters from '../../../common/filter/html2plainText';
import PlaylistService from '../../../common/service/playlistService';
import ItemService from '../../../common/service/data/itemService';
import template from './episodes.html!text';

@Module({
    name : 'ps.podcasts.details.episodes',
    modules : [ PlaylistService, HtmlFilters, ItemService ]
})
@Component({
    selector : 'podcast-items-list',
    as : 'pic',
    bindings : { podcast : '='},
    template : template
})
@Constant({ name : 'PodcastItemPerPage', value : 10 })
export default class PodcastItemsListComponent {

    currentPage = 1;

    constructor($scope, DonwloadManager, PodcastItemPerPage, itemService, playlistService, hotkeys) {
        "ngInject";
        /* DI */
        this.$scope = $scope;
        this.DownloadManager = DonwloadManager;
        this.itemPerPage = PodcastItemPerPage;
        this.itemService = itemService;
        this.playlistService = playlistService;
        this.hotkeys = hotkeys;
    }

    $onInit() {
        this.podcast.items = [];
        this.loadPage();

        this.subscription = this.DownloadManager
            .download$
            .filter(item => this.podcast.items.some(elem => elem.id === item.id))
            .subscribe(item => this.$scope.$evalAsync(() => Object.assign(this.podcast.items.find(elem => elem.id === item.id), item)));

        this.$scope.$on("podcastItems:refresh", () => { this.currentPage = 1; this.loadPage(); });

        this.hotkeys
            .bindTo(this.$scope)
            .add({ combo: 'right', description: 'Next page', callback: () => this.swipePage(1) })
            .add({ combo: 'left', description: 'Previous page', callback: () => this.swipePage(-1) });
    }

    $onDestroy() {
        this.subscription.dispose();
    }

    loadPage() {
        this.currentPage = (this.currentPage < 1) ? 1 : (this.currentPage > Math.ceil(this.totalItems / this.itemPerPage)) ? Math.ceil(this.totalItems / this.itemPerPage) : this.currentPage;
        return this.itemService.getItemForPodcastWithPagination(this.podcast, {
                size: this.itemPerPage,
                page : this.currentPage - 1,
                orders : [{ direction : 'DESC', property : 'pubDate'}]
            })
            .then(itemsResponse => { this.podcast.items = itemsResponse.content; this.podcast.totalItems = itemsResponse.totalElements; });
    }

    remove(item) {
        this.itemService.delete(item)
            .then(() => this.podcast.items = this.podcast.items.filter(elem => elem.id === item.id))
            .then(() => this.playlistService.remove(item))
            .then(() => this.loadPage());
    }

    swipePage(val) {
        this.currentPage += val;
        this.loadPage();
    }

    download(item) {
        return this.itemService.download(item);
    }

    stopDownload(item) {
        this.DownloadManager.stop(item);
    }

    toggleDownload(item) {
        this.DownloadManager.toggle(item);
    }

    play(item){
        return this.itemService.play(item);
    }
}
