/**
 * Created by kevin on 01/11/14 for Podcast Server
 */
import {Module, Service} from '../../../decorators';
import RestangularConfig from '../../../config/restangular.config';

@Module({
    name : 'ps.common.service.data.tagService',
    modules : [ RestangularConfig ]
})
@Service('tagService')
export default class tagService {

    constructor(Restangular) {
        "ngInject";
        this.baseAll = Restangular.all('tag');
    }

    getAll() {
        return this.baseAll.get();
    }

    search(query) {
        return this.baseAll.post(null, {name : query});
    }

}