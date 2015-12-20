import {Module, Config} from '../decorators';
import 'restangular';

@Module({
    name : 'ps.config.restangular',
    modules : [ 'restangular' ]
})
@Config((RestangularProvider) => { "ngInject"; RestangularProvider.setBaseUrl('/api/'); })
@Config((RestangularProvider) => {
    "ngInject"
    RestangularProvider.addElementTransformer('items', false, item => {
        item.addRestangularMethod('reset', 'get', 'reset');
        item.addRestangularMethod('download', 'get', 'addtoqueue');
        return item;
    });
})
export default class RestangularConfig {}