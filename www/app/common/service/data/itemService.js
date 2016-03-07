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

    search(searchParameters = { page : 0, size : 12, downloaded : true} ) {
        return this.$http.post(`/api/item/search`, searchParameters).then(r => r.data);
    }

    findById(podcastId, itemId) {
        return this.$http.get(`/api/podcast/${podcastId}/items/${itemId}`).then(r => r.data);
    }

    getItemForPodcastWithPagination(podcast, pageParemeters) {
        return this.$http.post(`/api/podcast/${podcast.id}/items`, pageParemeters).then(r => r.data);
    }

    delete(item) {
        return this.$http.delete(`/api/podcast/${item.podcastId}/items/${item.id}`);
    }

    reset(item) {
        return this.$http.get(`/api/podcast/${item.podcastId}/items/${item.id}/reset`).then(r => r.data);
    }

    download(item) {
        return this.$http.get(`/api/podcast/${item.podcastId}/items/${item.id}/addtoqueue`).then(r => r.data);
    }

    upload(podcast, file) {
        var formData = new FormData();
        formData.append('file', file);

        let config = {
            transformRequest: angular.identity,
            headers : {'Content-Type': undefined}
        };

        return this.$http
            .post(`/api/podcast/${podcast.id}/items/upload`, formData, config)
            .then(r => r.data);
    }

    play(item) {
        if (ItemService.isVideo(item)) {
            return this.$location.path(`/podcasts/${item.podcastId}/item/${item.id}/play`);
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