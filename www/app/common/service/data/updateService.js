import {Module, Service} from '../../../decorators';
import RestangularConfig from '../../../config/restangular.config';

@Module({
    name : 'ps.common.service.data.updateService',
    modules : [ RestangularConfig ]
})
@Service('UpdateService')
export default class UpdateService {
    constructor(Restangular) {
        "ngInject";
        this.Restangular = Restangular;
    }

    forceUpdatePodcast(idPodcast) {
        return this.Restangular.one("task").customPOST(idPodcast, "updateManager/updatePodcast/force");
    }
}

