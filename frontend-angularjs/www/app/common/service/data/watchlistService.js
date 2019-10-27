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
        return this.$http.post('/api/v1/playlists', watchlist).then(res => res.data);
    }

    findOne(id) {
        return this.$http.get(`/api/v1/playlists/${id}`).then(res => res.data);
    }

    findAll() {
        return this.$http.get('/api/v1/playlists').then(res => res.data).then(v => v.content);
    }

    delete(id){
        return this.$http.delete(`/api/v1/playlists/${id}`);
    }

    findAllWithItem(item) {
        return this.$http.get(`/api/v1/podcasts/${item.podcastId}/items/${item.id}/playlists`).then(res => res.data.content);
    }

    addItemToWatchList(watchlist, item) {
        return this.$http.post(`/api/v1/playlists/${watchlist.id}/items/${item.id}`);
    }

    removeItemFromWatchList(watchlist, item) {
        return this.$http.delete(`/api/v1/playlists/${watchlist.id}/items/${item.id}`);
    }
}
