/**
 * Created by kevin on 02/11/14.
 */

class podcastService  {
    
    constructor(Restangular) {
        this.Restangular = Restangular;
        this.route = 'podcast';
    }

    findById(podcastId) {
        return this.Restangular.one(this.route, podcastId).get();
    }

    findAll() {
        return this.Restangular.all(this.route).getList();
    }

    save(podcast) {
        return podcast.save();
    }

    getNewPodcast() {
        return this.Restangular.one(this.route);
    }

    patch(item) {
        return item.patch();
    }

    deletePodcast(item) {
        return item.remove();
    }

    findInfo(url) {
        return this.Restangular.one(this.route).findInfo(url);
    }

    statsByPubdate(id, numberOfMonth = 6) {
        return this.Restangular.one(this.route, id).one('stats').all('byPubdate').post(numberOfMonth);
    }

    statsByByDownloaddate(id, numberOfMonth = 6) {
        return this.Restangular.one(this.route, id).one('stats').all('byDownloaddate').post(numberOfMonth);
    }
    
}

angular.module('ps.dataService.podcast', ['restangular'])
    .config((RestangularProvider) => {
            RestangularProvider.addElementTransformer('podcast', false, (podcast) => {
                podcast.addRestangularMethod('findInfo', 'post', 'fetch', undefined, {'Content-Type': 'text/plain'});
                return podcast;
            });
        })
    .service('podcastService', podcastService);