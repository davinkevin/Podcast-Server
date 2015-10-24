import angular from 'angular';
import searchModule from './search/search';

let app = angular.module('podcastApp', [
    searchModule.name,
    'ps.podcasts',
    'ps.item',
    'ps.download',
    'ps.player',
    'ps.stats',
    'ps.config'
]);

angular.element(document).ready(() =>  angular.bootstrap(document.body, [ app.name ], { strictDi: false }));