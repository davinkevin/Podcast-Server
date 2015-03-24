/**
 * Created by kevin on 01/11/14.
 */

class itemService {
    constructor(Restangular) {
        this.Restangular = Restangular;
    }

    search(searchParameters = { page : 0, size : 12} ) {
        return this.Restangular.one("item/search")
            .post(null, searchParameters)
            .then((responseFromServer) => {
                responseFromServer.content = this.restangularizedItems(responseFromServer.content);
                return responseFromServer;
            });
    }

    findById(podcastId, itemId) {
        return this.Restangular.one("podcast", podcastId).one("items", itemId).get();
    }

    getItemForPodcastWithPagination(podcast, pageParemeters) {
        return podcast.one("items").post(null, pageParemeters);
    }

    restangularizePodcastItem (podcast, items) {
        return this.Restangular.restangularizeCollection(podcast, items, 'items');
    }

    restangularizedItems(itemList) {
        var restangularList = [];
        
        angular.forEach(itemList, (value) => {
            restangularList.push(this.Restangular.restangularizeElement(this.Restangular.one('podcast', value.podcastId), value, 'items'));
        });
        return restangularList;
    }
}

angular.module('ps.dataService.item', ['restangular']).service('itemService', itemService);
