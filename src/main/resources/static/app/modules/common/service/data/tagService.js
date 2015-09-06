/**
 * Created by kevin on 01/11/14 for Podcast Server
 */

class tagService {

    constructor(Restangular) {
        this.baseAll = Restangular.all('tag');
    }

    getAll() {
        return this.baseAll.get();
    }

    search(query) {
        return this.baseAll.post(null, {name : query});
    }

}

angular.module('ps.common.service.data.tagService', ['restangular'])
    .service('tagService', tagService);