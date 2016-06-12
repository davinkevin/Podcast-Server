import {Module, Config, Run} from '../decorators';
import 'angular-route';
import 'angular-hotkeys';

let registerGlobalHotkeys = ($location, hotkeys) => {
    "ngInject";

    [
        ['h', 'Goto Home', '/items'],
        ['p', 'Goto Podcast List','/podcasts'],
        ['d', 'Goto Download List', '/download']
    ]
        .map(hotkey => ({ combo: hotkey[0], description: hotkey[1], callback: () =>  $location.path(hotkey[2]) }))
        .forEach(hotkey => hotkeys.add(hotkey));
};

@Module({
    name : 'ps.config.route',
    modules : [ 'ngRoute', 'cfp.hotkeys']
})
@Run(registerGlobalHotkeys)
@Config($routeProvider => {"ngInject"; return $routeProvider.otherwise({redirectTo: '/items'});})
@Config($locationProvider => {"ngInject"; return $locationProvider.html5Mode(true);})
export default class RouteConfig {}