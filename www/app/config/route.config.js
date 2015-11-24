import angular from 'angular';
import 'angular-route';
import 'angular-hotkeys';

export default angular
    .module('ps.config.route', ['ngRoute','cfp.hotkeys'])
    .config($routeProvider => {"ngInject"; return $routeProvider.otherwise({redirectTo: '/items'});})
    .config($locationProvider => {"ngInject"; return $locationProvider.html5Mode(true);})
    .run(($location, hotkeys) => {
        "ngInject";

        let defaultKeys = [
            ['h', 'Goto Home', '/items'],
            ['s', 'Goto Search', '/item/search'],
            ['p', 'Goto Podcast List','/podcasts'],
            ['d', 'Goto Download List', '/download']
        ];

        for (let hotkey of defaultKeys) {
            hotkeys.add({
                combo: hotkey[0],
                description: hotkey[1],
                callback: () =>  $location.path(hotkey[2])
            });
        }
    });