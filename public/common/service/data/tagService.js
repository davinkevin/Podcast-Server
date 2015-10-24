/**
 * Created by kevin on 01/11/14 for Podcast Server
 */

import AppRestangularConfig from 'config/restangular.config';

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

export default angular
    .module('ps.common.service.data.tagService', [
        AppRestangularConfig.name
    ])
    .service('tagService', tagService);