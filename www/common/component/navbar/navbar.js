import angular from 'angular';
import NavbarComponent from './navbar.component';

export default angular.module('ps.common.component.navbar', [])
    .directive('navbar', NavbarComponent.component)
    .controller('navBarController', NavbarComponent);
