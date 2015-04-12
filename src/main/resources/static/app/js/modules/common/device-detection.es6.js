class deviceDetectorService {
    constructor($window) {
        this.$window = $window;
    }

    isTouchedDevice() {
        return 'ontouchstart' in this.$window;
    }
}

angular.module('device-detection', [])
    .service('deviceDetectorService', deviceDetectorService);