
class navbarController {
    constructor() {
        this.navCollapsed = true;
    }
}

class navbarDirective {
    constructor() {
        this.transclude = true;
        this.replace = true;
        this.restrict = 'E';
        this.scope = true;
        this.templateUrl = 'html/navbar.html';
        this.controller = 'navbarController';
        this.controllerAs = 'navbar';
    }

    link(scope, element) {
        element.removeClass('hidden');
    }
}

angular.module('navbar', [])
    .directive('navbar', () => new navbarDirective())
    .controller('navbarController', navbarController);
