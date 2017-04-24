import {Service, Module} from '../../decorators';

@Module({ name : 'ps.common.component.device-detection' })
@Service('deviceDetectorService')
export default class DeviceDetectorService {
    constructor($window) {
        "ngInject";
        this.$window = $window;
    }

    isTouchedDevice() {
        return 'ontouchstart' in this.$window;
    }
}