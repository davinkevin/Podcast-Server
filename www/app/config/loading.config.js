import angular from 'angular';
import 'angular-loading-bar';

export default angular
    .module('ps.config.loading', [ 'angular-loading-bar' ])
    .config((cfpLoadingBarProvider) =>  { "ngInject"; cfpLoadingBarProvider.includeSpinner = false; } );