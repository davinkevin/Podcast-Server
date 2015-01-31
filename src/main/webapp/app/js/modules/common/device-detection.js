angular.module('device-detection', [])
    .factory('deviceDetectorService', function deviceDetectorService($window) {
        return {
            isTouchedDevice : isTouchedDevice            
        };
        
        function isTouchedDevice() {
            return 'ontouchstart' in $window;
        }
    });