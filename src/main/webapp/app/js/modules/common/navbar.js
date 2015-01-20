angular.module('navbar', [
])
    .directive('navbar', function() {
        return {
            replace : true,
            restrict : 'E',
            templateUrl : 'html/navbar.html',
            scope : true,
            controllerAs : 'navbar',
            controller : 'navbarController'
        };
    }).controller('navbarController', function(){
        var vm = this;
        vm.navCollapsed = true;
    });
