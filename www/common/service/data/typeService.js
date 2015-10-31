/**
* Created by kevin on 01/11/14 for Podcast Server
*/
import RestangularConfig from '../../../config/restangular.config';

class typeService {

    constructor(Restangular) {
        "ngInject";
        this.baseAll = Restangular.all('types');
    }

    findAll() {
        return this.baseAll.getList();
    }
}

export default angular
    .module('ps.common.service.data.typeService', [
        RestangularConfig.name
    ])
    .service('typeService', typeService);