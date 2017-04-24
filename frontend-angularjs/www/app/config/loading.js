import {Module, Config} from '../decorators';
import 'angular-loading-bar';

@Module({
    name : 'ps.config.loading',
    modules : [ 'angular-loading-bar' ]
})
@Config(cfpLoadingBarProvider =>  { "ngInject"; cfpLoadingBarProvider.includeSpinner = false; })
export default class LoadingBar {}