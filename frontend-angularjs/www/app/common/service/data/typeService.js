/**
* Created by kevin on 01/11/14 for Podcast Server
*/
import {Module, Service} from '../../../decorators';

@Module({
    name : 'ps.common.service.data.typeService'
})
@Service('typeService')
export default class typeService {

    constructor($http) {
        "ngInject";
        this.$http = $http;
    }

    findAll() {
        return this.$http.get('/api/types').then(r => r.data);
    }
}