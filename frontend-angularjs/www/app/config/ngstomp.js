import {Module, Config} from '../decorators';
import SockJS from 'sockjs-client';
import AngularStompDKModule from 'AngularStompDK/core/ngStomp';

@Module({
    name : 'ps.config.ngstomp',
    modules : [ AngularStompDKModule ]
})
@Config(ngstompProvider => {"ngInject"; ngstompProvider.url('/ws').credential('login', 'password').class(SockJS);} )
export default class AngularStompDK {}