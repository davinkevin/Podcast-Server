/**
* Created by kevin on 01/11/14 for Podcast Server
*/

class typeService {

    constructor(Restangular) {
        this.baseAll = Restangular.all('types');
    }

    findAll() {
        return this.baseAll.getList();
    }
}

angular.module('ps.common.service.data.typeService', ['restangular'])
    .service('typeService', typeService);