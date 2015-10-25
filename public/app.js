import angular from 'angular';
import SearchModule from './search/search';
import PodcastsModule from './podcasts/podcasts';
import ItemModule from './item/item';
import DownloadModule from './download/download';

let app = angular.module('podcastApp', [
    SearchModule.name,
    PodcastsModule.name,
    ItemModule.name,
    'ps.download',
    'ps.player',
    'ps.stats',
    'ps.config'
]);

angular.element(document).ready(() =>  angular.bootstrap(document.body, [ app.name ], { strictDi: false }));