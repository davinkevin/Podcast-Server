angular.module('ps.config.loading', [
    'angular-loading-bar'
])
    .config(function (cfpLoadingBarProvider) {
        cfpLoadingBarProvider.includeSpinner = false;
    });