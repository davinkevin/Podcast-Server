angular.module('podcastApp', [
    'ps.search',
    'ps.podcast',
    'ps.item',
    'ps.download',
    'ps.partial',
    'ps.filters',
    'ngRoute',
    'ngTouch',
    'cfp.hotkeys',
    'restangular',
    'AngularStomp',
    'LocalStorageModule',
    'ngAnimate',
    'truncate',
    'ui.bootstrap',
    'angular-loading-bar',
    'ngTagsInput',
    'notification'
])
    .config(function($routeProvider) {

        var commonKey = [
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
        ];

            $routeProvider.
                when('/podcasts', {
                    templateUrl: 'html/podcasts-list.html',
                    controller: 'PodcastsListCtrl',
                    hotkeys: commonKey
                }).
                when('/podcast/add', {
                    templateUrl: 'html/podcast-add.html',
                    controller: 'PodcastAddCtrl',
                    hotkeys: commonKey
                }).
                when('/podcast/:podcastId', {
                    templateUrl: 'html/podcast-detail.html',
                    controller: 'PodcastDetailCtrl',
                    hotkeys: [
                        ['r', 'Refresh', 'refreshItems()'],
                        ['f', 'Force Refresh', 'refresh()'],
                        ['l', 'List of Items', 'tabs[0].active = true'],
                        ['m', 'Modification of Podcast', 'tabs[1].active = true']
                    ].concat(commonKey),
                    resolve : {
                        podcast : function (Restangular, $route) {
                            return Restangular.one('podcast', $route.current.params.podcastId).get();
                        }
                    }
                }).
                when('/items', {
                    templateUrl: 'html/items-search.html',
                    controller: 'ItemsSearchCtrl',
                    reloadOnSearch: false,
                    hotkeys: [
                        ['right', 'Next page', 'currentPage = currentPage+1; changePage();'],
                        ['left', 'Previous page', 'currentPage = currentPage-1; changePage();']
                    ].concat(commonKey)
                }).
                when('/podcast/:podcastId/item/:itemId', {
                    templateUrl: 'html/item-detail.html',
                    controller: 'ItemDetailCtrl',
                    hotkeys: commonKey
                }).
                when('/download', {
                    templateUrl: 'html/download.html',
                    controller: 'DownloadCtrl',
                    hotkeys: commonKey
                }).
                otherwise({
                    redirectTo: '/items'
                });
        })
    .config(function (cfpLoadingBarProvider) {
        cfpLoadingBarProvider.includeSpinner = false;
    })
    .config(function(RestangularProvider) {
        RestangularProvider.setBaseUrl('/api/');

        RestangularProvider.addElementTransformer('items', false, function(item) {
            item.addRestangularMethod('reset', 'get', 'reset');
            item.addRestangularMethod('download', 'get', 'addtoqueue');
            return item;
        });
    })
    .config(function($tooltipProvider) {
        //TODO : fix for problem in angular 1.3.0 : https://github.com/angular-ui/bootstrap/issues/2828
        $tooltipProvider.options({animation: false});
    });