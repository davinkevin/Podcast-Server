/**
* Created by kevin on 01/11/14 for Podcast Server
*/
import AppRestangularConfig from 'config/restangular.config';

class StatService {

    constructor(Restangular) {
        this.Restangular = Restangular;
        this.base = this.Restangular.one('stats');
    }

    statsByType(numberOfMonth = 1) {
        return this.base.all('byType').post(numberOfMonth);
    }
}

export default angular
    .module('ps.common.service.data.statsService', [
        AppRestangularConfig.name
    ])
    .service('statService', StatService);
