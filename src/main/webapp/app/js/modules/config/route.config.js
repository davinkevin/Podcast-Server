angular.module('ps.config.route', [
    'ngRoute',
    'cfp.hotkeys'
])
    .constant('commonKey', [
        ['h', 'Goto Home', function (event) {
            event.preventDefault();
            window.location.href = '#/items';
        }],
        ['s', 'Goto Search', function (event) {
            event.preventDefault();
            window.location.href = '#/item/search';
        }],
        ['p', 'Goto Podcast List', function (event) {
            event.preventDefault();
            window.location.href = '#/podcasts';
        }],
        ['d', 'Goto Download List', function (event) {
            event.preventDefault();
            window.location.href = '#/download';
        }]
    ])
    .config(function($routeProvider, commonKey) {
        /*$routeProvider.
            when('/podcast/add', {
                templateUrl: 'html/podcast-add.html',
                controller: 'PodcastAddCtrl',
                hotkeys: commonKey
            });*/
        
        $routeProvider.
            otherwise({
                redirectTo: '/items'
            });
    });