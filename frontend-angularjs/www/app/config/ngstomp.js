import {Module, Config} from '../decorators';
import SockJS from 'sockjs-client';
import AngularStompDKModule from 'AngularStompDK/core/ngStomp';

@Module({
    name : 'ps.config.ngstomp',
    modules : [ AngularStompDKModule ]
})
@Config(ngstompProvider => {"ngInject"; ngstompProvider.url('/ws/sockjs').credential('login', 'password').class(SockJS);} )
export default class AngularStompDK {}