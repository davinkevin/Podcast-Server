angular.module('podcastApp', [
    'podcast.controller',
    'podcast.filters',
    'podcast.services',
    'podcast.partial',
    'ngRoute',
    'cfp.hotkeys',
    'restangular',
    'AngularStomp',
    'LocalStorageModule',
    'ngAnimate',
    'truncate',
    'ui.bootstrap',
    'angular-loading-bar',
    'ngTagsInput'
])
    .config(function($routeProvider) {
            $routeProvider.
                when('/podcasts', {
                    templateUrl: 'html/podcasts-list.html',
                    controller: 'PodcastsListCtrl',
                    hotkeys: [
                        ['h', 'Goto Home', function (event) {
                            event.preventDefault();
                            window.location.href = '#/items';
                        }, false],
                        ['s', 'Goto Search', function (event) {
                            event.preventDefault();
                            window.location.href = '#/item/search';
                        }, false],
                        ['p', 'Goto Podcast List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/podcasts';
                        }, false],
                        ['d', 'Goto Download List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/download';
                        }, false]
                    ]
                }).
                when('/podcast/add', {
                    templateUrl: 'html/podcast-add.html',
                    controller: 'PodcastAddCtrl',
                    hotkeys: [
                        ['h', 'Goto Home', function (event) {
                            event.preventDefault();
                            window.location.href = '#/items';
                        }, false],
                        ['s', 'Goto Search', function (event) {
                            event.preventDefault();
                            window.location.href = '#/item/search';
                        }, false],
                        ['p', 'Goto Podcast List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/podcasts';
                        }, false],
                        ['d', 'Goto Download List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/download';
                        }, false]
                    ]
                }).
                when('/podcast/:podcastId', {
                    templateUrl: 'html/podcast-detail.html',
                    controller: 'PodcastDetailCtrl',
                    hotkeys: [
                        ['r', 'Refresh', 'refreshItems()', false],
                        ['f', 'Force Refresh', 'refresh()', false],
                        ['l', 'List of Items', 'tabs[0].active = true', false],
                        ['m', 'Modification of Podcast', 'tabs[1].active = true', false],
                        ['h', 'Goto Home', function (event) {
                            event.preventDefault();
                            window.location.href = '#/items';
                        }, false],
                        ['s', 'Goto Search', function (event) {
                            event.preventDefault();
                            window.location.href = '#/item/search';
                        }, false],
                        ['p', 'Goto Podcast List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/podcasts';
                        }, false],
                        ['d', 'Goto Download List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/download';
                        }, false]
                    ]
                }).
                when('/items', {
                    templateUrl: 'html/items-list.html',
                    controller: 'ItemsListCtrl',
                    hotkeys: [
                        ['right', 'Next page', 'currentPage = currentPage+1; changePage();', false],
                        ['left', 'Previous page', 'currentPage = currentPage-1; changePage();', false],
                        ['h', 'Goto Home', function (event) {
                            event.preventDefault();
                            window.location.href = '#/items';
                        }, false],
                        ['s', 'Goto Search', function (event) {
                            event.preventDefault();
                            window.location.href = '#/item/search';
                        }, false],
                        ['p', 'Goto Podcast List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/podcasts';
                        }, false]                      ,
                        ['d', 'Goto Download List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/download';
                        }, false]
                    ]
                }).
                when('/item/search', {
                    templateUrl: 'html/items-search.html',
                    controller: 'ItemsSearchCtrl',
                    hotkeys: [
                        ['right', 'Next page', 'currentPage = currentPage+1; changePage();', false],
                        ['left', 'Previous page', 'currentPage = currentPage-1; changePage();', false],
                        ['h', 'Goto Home', function (event) {
                            event.preventDefault();
                            window.location.href = '#/items';
                        }, false],
                        ['s', 'Goto Search', function (event) {
                            event.preventDefault();
                            window.location.href = '#/item/search';
                        }, false],
                        ['p', 'Goto Podcast List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/podcasts';
                        }, false],
                        ['d', 'Goto Download List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/download';
                        }, false]
                    ]
                }).
                when('/item/:itemId', {
                    templateUrl: 'html/item-detail.html',
                    controller: 'ItemDetailCtrl',
                    hotkeys: [
                        ['h', 'Goto Home', function (event) {
                            event.preventDefault();
                            window.location.href = '#/items';
                        }, false],
                        ['s', 'Goto Search', function (event) {
                            event.preventDefault();
                            window.location.href = '#/item/search';
                        }, false],
                        ['p', 'Goto Podcast List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/podcasts';
                        }, false],
                        ['d', 'Goto Download List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/download';
                        }, false],
                    ]
                }).
                when('/download', {
                    templateUrl: 'html/download.html',
                    controller: 'DownloadCtrl',
                    hotkeys: [
                        ['h', 'Goto Home', function (event) {
                            event.preventDefault();
                            window.location.href = '#/items';
                        }, false],
                        ['s', 'Goto Search', function (event) {
                            event.preventDefault();
                            window.location.href = '#/item/search';
                        }, false],
                        ['p', 'Goto Podcast List', function (event) {
                            event.preventDefault();
                            window.location.href = '#/podcasts';
                        }, false]
                    ]
                }).
                otherwise({
                    redirectTo: '/items'
                });
        })
    .config(['cfpLoadingBarProvider', function (cfpLoadingBarProvider) {
        cfpLoadingBarProvider.includeSpinner = false;
    }])
    .config(function(RestangularProvider) {
        RestangularProvider.setBaseUrl('/api/');
});