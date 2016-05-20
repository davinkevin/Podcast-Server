/**
    * Created by kevin on 20/02/2016 for Podcast Server
    */
import {Component, Module} from '../../../../decorators';
import './vg-link.css!';

@Module({
    name : 'ps.common.component.videogular.vgLink'
})
@Component({
    selector : 'vg-link',
    as : 'vglink',
    bindings : { url: '='},
    template : `<div class="btn-video-share"><a target="_self" ng-href="{{ vglink.url }}" class="ionicons ion-android-share"></a></div>`
})
export default class VgLink {}

