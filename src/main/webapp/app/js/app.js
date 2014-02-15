var podcastApp = angular.module('podcastApp', [
    'ngRoute',
    'podcastControllers',
    'podcastFilters',
    'podcastServices',
    'restangular',
    'ui.bootstrap'
]);

podcastApp.config(['$routeProvider',
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
    }]);

podcastApp.config(function(RestangularProvider) {
    RestangularProvider.setBaseUrl('/api/');
});