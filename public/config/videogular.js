/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */

import angular from 'angular';
import 'angular-sanitize';
import Videogular from 'videogular';
import VideogularBuffering from 'videogular-buffering';
import VideogularControls from 'videogular-controls';
import VideogularPoster from 'videogular-poster';
import VideogularOverlayPlay from 'videogular-overlay-play';
import '/jspm_packages/github/2fdevs/bower-videogular-themes-default@1.3.2/videogular.css!';

export default angular.module('ps.config.videogular', [
    'ngSanitize',
    Videogular,
    VideogularPoster,
    VideogularControls,
    VideogularOverlayPlay,
    VideogularBuffering
]);