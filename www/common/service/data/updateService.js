
import RestangularConfig from '../../../config/restangular.config';

class UpdateService {
    constructor(Restangular) {
        "ngInject";
        this.Restangular = Restangular;
    }

    forceUpdatePodcast(idPodcast) {
        return this.Restangular.one("task").customPOST(idPodcast, "updateManager/updatePodcast/force");
    }
} 

export default angular
    .module('ps.common.service.data.updateService', [
        RestangularConfig.name
    ])
    .service('UpdateService', UpdateService);