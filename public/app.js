import angular from 'angular';
import SearchModule from './search/search';
import PodcastsModule from './podcasts/podcasts';
import ItemModule from './item/item';
import DownloadModule from './download/download';
import PlayerModule from './player/player';

let app = angular.module('podcastApp', [
    SearchModule.name,
    PodcastsModule.name,
    ItemModule.name,
    DownloadModule.name,
    PlayerModule.name,
    'ps.stats',
    'ps.config'
]);

angular.element(document).ready(() =>  angular.bootstrap(document.body, [ app.name ], { strictDi: false }));