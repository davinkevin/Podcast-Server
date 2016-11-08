/**
 * Created by kevin on 31/01/2016.
 */
import {Module, Service} from '../../../decorators';

@Module({
    name : 'ps.common.service.watchlist'
})
@Service('WatchListService')
export default class WatchListService {

    constructor($http) {
        "ngInject";
        this.$http = $http;
    }

    create(watchlist) {
        return this.$http.post('/api/watchlists', watchlist).then(res => res.data);
    }

    findOne(id) {
        return this.$http.get(`/api/watchlists/${id}`).then(res => res.data);
    }

    findAll() {
        return this.$http.get('/api/watchlists').then(res => res.data);
    }

    delete(id){
        return this.$http.delete(`/api/watchlists/${id}`);
    }

    findAllWithItem(item) {
        return this.$http.get(`/api/podcasts/${item.podcastId}/items/${item.id}/watchlists`).then(res => res.data);
    }

    addItemToWatchList(watchlist, item) {
        return this.$http.post(`/api/watchlists/${watchlist.id}/${item.id}`);
    }

    removeItemFromWatchList(watchlist, item) {
        return this.$http.delete(`/api/watchlists/${watchlist.id}/${item.id}`);
    }
}