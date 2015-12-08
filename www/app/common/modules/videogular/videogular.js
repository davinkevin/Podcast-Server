/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */

import 'angular-sanitize';
import {Module} from '../../../decorators';
import VideogularModule from 'videogular';
import VideogularBuffering from 'videogular-buffering';
import VideogularControls from 'videogular-controls';
import VideogularPoster from 'videogular-poster';
import VideogularOverlayPlay from 'videogular-overlay-play';
import 'videogular-themes-default/videogular.css!';
import './videogular.css!';

@Module({
    name : 'ps.config.videogular',
    modules : [ 'ngSanitize', VideogularModule, VideogularPoster, VideogularControls, VideogularOverlayPlay, VideogularBuffering ]
})
export default class Videogular{}