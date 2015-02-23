angular.module('navbar', [
])
    .directive('navbar', function() {
        return {
            transclude : true,
            replace : true,
            restrict : 'E',
            templateUrl : 'html/navbar.html',
            scope : true,
            controllerAs : 'navbar',
            controller : 'navbarController',
            link : function(scope, element) {
                element.removeClass('hidden');
            }
        };
    }).controller('navbarController', function(){
        var vm = this;
        vm.navCollapsed = true;
    });
