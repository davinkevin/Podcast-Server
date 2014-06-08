angular.module('podcastApp', [
    'podcast.controller',
    'podcast.filters',
    'podcast.services',
    'podcast.partial',
    'ngRoute',
    'restangular',
    'AngularStomp',
    'LocalStorageModule',
    'ngAnimate',
    'truncate',
    'ui.bootstrap',
    'angular-loading-bar',
    'ngTagsInput'
])
    .config(['$routeProvider',
        function($routeProvider) {
            $routeProvider.
                when('/podcasts', {
                    templateUrl: 'html/podcasts-list.html',
                    controller: 'PodcastsListCtrl'
                }).
                when('/podcast/add', {
                    templateUrl: 'html/podcast-add.html',
                    controller: 'PodcastAddCtrl'
                }).
                when('/podcast/:podcastId', {
                    templateUrl: 'html/podcast-detail.html',
                    controller: 'PodcastDetailCtrl'
                }).
                when('/items', {
                    templateUrl: 'html/items-list.html',
                    controller: 'ItemsListCtrl'
                }).
                when('/item/:itemId', {
                    templateUrl: 'html/item-detail.html',
                    controller: 'ItemDetailCtrl'
                }).
                when('/download', {
                    templateUrl: 'html/download.html',
                    controller: 'DownloadCtrl'
                }).
                otherwise({
                    redirectTo: '/items'
                });
        }])
    .config(['cfpLoadingBarProvider', function (cfpLoadingBarProvider) {
        cfpLoadingBarProvider.includeSpinner = false;
    }])
    .config(function(RestangularProvider) {
        RestangularProvider.setBaseUrl('/api/');
});