
class navBarController {
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
        this.templateUrl = 'common/component/navbar/navbar.html';
        this.controller = 'navBarController';
        this.controllerAs = 'navbar';
    }

    link(scope, element) {
        element.removeClass('hidden');
    }
}

angular.module('ps.common.component.navbar', [])
    .directive('navbar', () => new navbarDirective())
    .controller('navBarController', navBarController);
