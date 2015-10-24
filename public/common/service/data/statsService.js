/**
* Created by kevin on 01/11/14 for Podcast Server
*/

class statService {

    constructor(Restangular) {
        this.Restangular = Restangular;
        this.base = this.Restangular.one('stats');
    }

    statsByType(numberOfMonth = 1) {
        return this.base.all('byType').post(numberOfMonth);
    }
}

angular.module('ps.common.service.data.statsService', ['restangular'])
    .service('statService', statService);
