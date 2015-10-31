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
import 'videogular-themes-default/videogular.css!';

export default angular.module('ps.config.videogular', [
    'ngSanitize',
    Videogular,
    VideogularPoster,
    VideogularControls,
    VideogularOverlayPlay,
    VideogularBuffering
]);