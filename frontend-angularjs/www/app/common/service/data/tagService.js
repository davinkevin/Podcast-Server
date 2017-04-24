/**
 * Created by kevin on 01/11/14 for Podcast Server
 */
import {Module, Service} from '../../../decorators';

@Module({
    name : 'ps.common.service.data.tagService'
})
@Service('tagService')
export default class tagService {

    constructor($http) {
        "ngInject";
        this.$http = $http;
    }

    search(query) {
        return this.$http.post('/api/tags', null, { params : {name : query} });
    }
}