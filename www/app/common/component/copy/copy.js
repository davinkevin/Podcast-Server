/**
    * Created by kevin on 12/12/2015 for Podcast Server
    */

import {Directive, Module} from '../../../decorators';
import Clipboard from 'clipboard';

@Module({
    name : 'ps.common.component.copy'
})
@Directive({
    selector : 'copy',
    bindToController : { copy: '@'},
    as : 'c'
})
export default class Copy {

    constructor($window) {
        "ngInject";
        this.baseUrl = $window.location.origin;
    }

    get url() {
        return this.copy.substring(0, 1) === '/' ? this.baseUrl + this.copy : this.copy;
    }

    static link(scope, element, _, ctrl) {
        let clipboard = new Clipboard(element[0], { text: () => ctrl.url});
        scope.$on('destroy', () => clipboard.destroy());
    }
}