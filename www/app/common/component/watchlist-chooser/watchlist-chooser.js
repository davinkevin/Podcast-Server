/**
* Created by kevin on 31/01/2016 for PodcastServer
*/
import {Module, UibModal} from '../../../decorators';
import WatchListServiceModule from '../../service/data/watchlistService';
import template from './watchlist-chooser.html!text';
import './watchlist-chooser.css!';

@Module({
    name : 'ps.common.component.watchlist-chooser',
    modules : [ WatchListServiceModule ]
})
@UibModal({
    as : 'wlc',
    size : 'sm',
    backdrop : true,
    resolve : { watchLists : WatchListService => {"ngInject"; return WatchListService.findAll();}},
    template : template
})
export default class WatchlistChooser {

    constructor(item, watchListsOfItem, watchLists, $uibModalInstance, WatchListService) {
        "ngInject";
        this.$uibModalInstance = $uibModalInstance;
        this.watchListService = WatchListService;

        this.item = item;
        this.watchListsOfItem = WatchlistChooser.extractIdOfPlaylist(watchListsOfItem);
        this.watchLists = watchLists;
    }

    create(name) {
        return this.watchListService
            .create({ name })
            .then(p => this.select(p));
    }

    select(watchlist) {
        return this.watchListService
            .addItemToWatchList(watchlist, this.item)
            .then(() => this.$uibModalInstance.close());
    }

    remove(watchlist) {
        return this.watchListService
            .removeItemFromWatchList(watchlist, this.item)
            .then(() => this.watchListService.findAllWithItem(this.item))
            .then(woi => this.watchListsOfItem = WatchlistChooser.extractIdOfPlaylist(woi));
    }

    isInPlaylist(watchlist) {
        return this.watchListsOfItem.indexOf(watchlist.id) !== -1;
    }

    static extractIdOfPlaylist(watchList) {
        return watchList.map(w => w.id);
    }
}
