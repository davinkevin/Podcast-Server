/**
* Created by kevin on 01/11/14 for Podcast Server
*/
import {Module, Service} from '../../../decorators';
import RestangularConfig from '../../../config/restangular.config';

@Module({
    name : 'ps.common.service.data.typeService',
    modules : [ RestangularConfig ]
})
@Service('typeService')
export default class typeService {

    constructor(Restangular) {
        "ngInject";
        this.baseAll = Restangular.all('types');
    }

    findAll() {
        return this.baseAll.getList();
    }
}