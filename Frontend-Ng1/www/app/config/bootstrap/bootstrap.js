/**
    * Created by kevin on 25/10/2015 for PodcastServer
    */

import './bootstrap.css!';
import uiBootstrap from 'angular-bootstrap';
import {Module} from '../../decorators';

@Module({
    name : 'ps.config.bootstrap',
    modules : [ uiBootstrap ]
})
export default class Bootstrap{}