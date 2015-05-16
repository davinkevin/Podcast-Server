class UpdatingDirective {
    constructor() {
        this.restrict = 'E';
        this.scope = true;
        this.templateUrl = 'html/updating.html';
        this.controller = 'UpdatingController';
        this.controllerAs = 'uc';
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

angular.module('updating', ['AngularStompDK'])
    .directive('updateStatus', () => new UpdatingDirective())
    .controller('UpdatingController', UpdatingController);
