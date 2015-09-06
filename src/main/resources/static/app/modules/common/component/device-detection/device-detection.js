class deviceDetectorService {
    constructor($window) {
        this.$window = $window;
    }

    isTouchedDevice() {
        return 'ontouchstart' in this.$window;
    }
}

angular.module('ps.common.component.device-detection', [])
    .service('deviceDetectorService', deviceDetectorService);