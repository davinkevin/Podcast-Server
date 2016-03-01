/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */

import angular from 'angular';
import 'angular-sanitize';
import {Module, Service} from '../../../decorators';
import DeviceDetectionService from '../../service/device-detection';
import VideogularModule from 'videogular';
import VideogularBuffering from 'videogular-buffering';
import VideogularControls from 'videogular-controls';
import VideogularPoster from 'videogular-poster';
import VideogularOverlayPlay from 'videogular-overlay-play';
import VgCopy from './vg-copy/vg-copy';
import VgLink from './vg-link/vg-link';
import 'videogular-themes-default/videogular.css!';
import './videogular.css!';

@Module({
    name : 'ps.common.component.videogular',
    modules : [ 'ngSanitize', VideogularModule, VideogularPoster, VideogularControls, VideogularOverlayPlay, VideogularBuffering, VgCopy, VgLink, DeviceDetectionService ]
})
@Service('VideogularService')
export default class VideogularService {

    static defaultPlayerConfig = {
        autoPlay: true,
        sources: [],
        plugins: { controls: { autoHideTime: 2000 },
            poster: ''
        }
    };

    constructor(deviceDetectorService) {
        "ngInject";
        this.deviceDetectorService = deviceDetectorService;
    }

    builder() {
        return new VideogularConfigBuilder(VideogularService.defaultPlayerConfig, this.deviceDetectorService.isTouchedDevice());
    }
}


class VideogularConfigBuilder {

    constructor(config, isTouchDevice) {
        this.config = angular.copy(config);
        this.config.plugins.controls.autoHide = !isTouchDevice;
    }

    withItem(item) {
        this.config.sources.push({
            src : item.proxyURL,
            type : item.mimeType
        });
        this.config.plugins.poster = item.cover.url;
        return this;
    }

    build() {
        return this.config;
    }
}