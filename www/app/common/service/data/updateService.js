import {Module, Service} from '../../../decorators';

@Module({
    name : 'ps.common.service.data.updateService'
})
@Service('UpdateService')
export default class UpdateService {
    constructor($http) {
        "ngInject";
        this.$http = $http;
    }

    forceUpdatePodcast(idPodcast) {
        return this.$http.post('/api/task/updateManager/updatePodcast/force', idPodcast);
    }
}

