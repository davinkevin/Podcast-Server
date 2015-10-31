import 'angular-loading-bar';

export default angular
    .module('ps.config.loading', [ 'angular-loading-bar' ])
    .config((cfpLoadingBarProvider) =>  { "ngInject"; return cfpLoadingBarProvider.includeSpinner = false; } );