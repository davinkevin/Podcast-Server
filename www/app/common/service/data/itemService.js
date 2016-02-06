/**
 * Created by kevin on 01/11/14 for PodcastServer
 */
import angular from 'angular';
import {Module, Service} from '../../../decorators';
import RestangularConfig from '../../../config/restangular';
import PlaylistService from '../playlistService';

@Module({
    name : 'ps.common.service.data.itemService',
    modules : [ RestangularConfig, PlaylistService]
})
@Service('itemService')
export default class ItemService {

    childRoute = "items";

    constructor(Restangular, $window, $location, playlistService) {
        "ngInject";
        this.Restangular = Restangular;
        this.$window = $window;
        this.$location = $location;
        this.playlistService = playlistService;
    }

    search(searchParameters = { page : 0, size : 12, downloaded : true} ) {
        return this.Restangular.one("item/search")
            .post(null, searchParameters)
            .then((responseFromServer) => {
                responseFromServer.content = this.restangularizedItems(responseFromServer.content);
                return responseFromServer;
            });
    }

    findById(podcastId, itemId) {
        return this.Restangular.one("podcast", podcastId).one(this.childRoute, itemId).get();
    }

    getItemForPodcastWithPagination(podcast, pageParemeters) {
        return podcast.one("items").post(null, pageParemeters);
    }

    restangularizePodcastItem (podcast, items) {
        return this.Restangular.restangularizeCollection(podcast, items, this.childRoute);
    }

    restangularizedItems(itemList) {
        var restangularList = [];

        angular.forEach(itemList, (value) => {
            restangularList.push(this.Restangular.restangularizeElement(this.Restangular.one('podcast', value.podcastId), value, this.childRoute));
        });
        return restangularList;
    }

    upload(podcast, file) {
        var formData = new FormData();
        formData.append('file', file);
        return podcast.all(this.childRoute)
            .withHttpConfig({transformRequest: angular.identity})
            .customPOST(formData, 'upload', undefined, {'Content-Type': undefined});
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