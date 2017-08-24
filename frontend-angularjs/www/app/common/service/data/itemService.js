/**
 * Created by kevin on 01/11/14 for PodcastServer
 */
import angular from 'angular';
import {Module, Service} from '../../../decorators';
import PlaylistService from '../playlistService';

@Module({
    name : 'ps.common.service.data.itemService',
    modules : [ PlaylistService]
})
@Service('itemService')
export default class ItemService {

    constructor($window, $location, playlistService, $http) {
        "ngInject";
        this.$http = $http;
        this.$window = $window;
        this.$location = $location;
        this.playlistService = playlistService;
    }

    search(searchParams = { page : 0, size : 12, status : ['FINISH'], tags: []} ) {
        let params = Object.assign({}, searchParams);
        params.sort = params.sort.map(o => `${o.property},${o.direction}`);
        params.tags = params.tags.map(t => t.name).join();
        params.status = params.status.join();
        return this.$http.get(`/api/items/search`, { params }).then(r => r.data);
    }

    findById(podcastId, itemId) {
        return this.$http.get(`/api/podcasts/${podcastId}/items/${itemId}`).then(r => r.data);
    }

    getItemForPodcastWithPagination(podcast, pageParams) {
        let params = Object.assign({}, pageParams);
        params.sort = params.sort.map(o => `${o.property},${o.direction}`);
        return this.$http.get(`/api/podcasts/${podcast.id}/items`, { params }).then(r => r.data);
    }

    delete(item) {
        return this.$http.delete(`/api/podcasts/${item.podcastId}/items/${item.id}`);
    }

    reset(item) {
        return this.$http.get(`/api/podcasts/${item.podcastId}/items/${item.id}/reset`).then(r => r.data);
    }

    download(item) {
        return this.$http.get(`/api/podcasts/${item.podcastId}/items/${item.id}/addtoqueue`).then(r => r.data);
    }

    upload(podcast, file) {
        var formData = new FormData();
        formData.append('file', file);

        let config = {
            transformRequest: angular.identity,
            headers : {'Content-Type': undefined}
        };

        return this.$http
            .post(`/api/podcasts/${podcast.id}/items/upload`, formData, config)
            .then(r => r.data);
    }

    play(item) {
        if (ItemService.isVideo(item)) {
            return this.$location.path(`/podcasts/${item.podcastId}/items/${item.id}/play`);
        }

        return this.playlistService.play(item);
    }

    static isVideo(item) {
        return (item.mimeType) ? item.mimeType.indexOf('video') !== -1 : false;
    }

    static isAudio(item) {
        return (item.mimeType) ? item.mimeType.indexOf('audio') !== -1 : false;
    }
}