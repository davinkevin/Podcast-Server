/**
    * Created by kevin on 20/02/2016 for Podcast Server
    */
import {Component, Config, Module} from '../../../../decorators';
import './vg-link-vlc.css!';

@Module({
    name : 'ps.common.component.videogular.vgLinkVlc'
})
@Config($compileProvider => { "ngInject"; return $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|vlc-x-callback):/); })
@Component({
    selector : 'vg-link-vlc',
    as : 'vglinkvlc',
    bindings : { url: '=' },
    template : `<div class="btn-video-share"><a target="_self" ng-href="vlc-x-callback://x-callback-url/stream?url={{ vglinkvlc.fqURL }}" class="ionicons ion-qr-scanner"></a></div>`
})
export default class VgLinkVLC {

    fqURL = ""

    constructor($location) {
        "ngInject";
        this.$location = $location;
    }

    $onInit() {
        this.fqURL = this.$location.protocol() + "://" + this.$location.host() + this.url
    }
}

