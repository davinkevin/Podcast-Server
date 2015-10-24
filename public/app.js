import angular from 'angular';
import SearchModule from './search/search';
import PodcastsModule from './podcasts/podcasts';

let app = angular.module('podcastApp', [
    SearchModule.name,
    PodcastsModule.name,
    'ps.item',
    'ps.download',
    'ps.player',
    'ps.stats',
    'ps.config'
]);

angular.element(document).ready(() =>  angular.bootstrap(document.body, [ app.name ], { strictDi: false }));