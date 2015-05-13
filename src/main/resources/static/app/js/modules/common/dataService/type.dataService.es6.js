/**
 * Created by kevin on 01/11/14.
 */

class typeService {

    constructor(Restangular) {
        this.baseAll = Restangular.all('types');
    }

    findAll() {
        return this.baseAll.getList();
    }
}

angular.module('ps.dataService.type', ['restangular'])
    .service('typeService', typeService);