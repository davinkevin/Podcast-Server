class UpdatingDirective {
    constructor() {
        this.restrict = 'E';
        this.scope = true;
        this.templateUrl = 'common/component/updating/updating.html';
        this.controller = 'UpdatingController';
        this.controllerAs = 'uc';
    }

    link(scope, element) {

        let liParent = element.parent().parent()[0];

        if (liParent && liParent.tagName === 'LI') {
            let liElement = angular.element(liParent);
            liElement.addClass('hidden');
            scope.$watch(
                'uc.isUpdating',
                (newValue) => (newValue) ? liElement.removeClass('hidden') : liElement.addClass('hidden')
            );
        }
    }
}

class UpdatingController {
    constructor(ngstomp, $scope) {
        this.ngstomp = ngstomp;
        this.isUpdating = false;
        this.$scope = $scope;

        this.ngstomp
            .subscribe('/app/updating', (message) => this.updateStatus(message), $scope)
            .subscribe('/topic/updating', (message) => this.updateStatus(message), $scope);
    }

    updateStatus(message) {
        this.isUpdating = JSON.parse(message.body);
    }
}

angular.module('ps.common.component.updating', ['AngularStompDK'])
    .directive('updateStatus', () => new UpdatingDirective())
    .controller('UpdatingController', UpdatingController);
