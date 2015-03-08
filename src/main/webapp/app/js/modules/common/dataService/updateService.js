
class UpdateService {
    constructor(Restangular) {
        this.Restangular = Restangular;
    }

    forceUpdatePodcast(idPodcast) {
        return this.Restangular.one("task").customPOST(idPodcast, "updateManager/updatePodcast/force");
    }
} 

angular.module('ps.dataService.updateService', ['restangular']).service('UpdateService', UpdateService);