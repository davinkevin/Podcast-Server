import {Module, Service} from '../../../decorators';
import RestangularConfig from '../../../config/restangular';

@Module({
    name : 'ps.common.service.data.updateService',
    modules : [ RestangularConfig ]
})
@Service('UpdateService')
export default class UpdateService {
    constructor(Restangular) {
        "ngInject";
        this.baseTask = Restangular.one("task");
    }

    forceUpdatePodcast(idPodcast) {
        return this.baseTask.customPOST(idPodcast, "updateManager/updatePodcast/force");
    }
}

