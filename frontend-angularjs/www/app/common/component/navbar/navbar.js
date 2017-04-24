/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../../../decorators';
import template from './navbar.html!text';
import './navbar.css!';

@Module({
    name : 'ps.common.component.navbar'
})
@Component({
    selector : 'navbar',
    as : 'navbar',
    replace : true,
    template : template,
    transclude : true
})
export default class NavbarComponent {
    constructor($element) {
        "ngInject";
        this.navCollapsed = true;
        $element.removeClass('hidden');
    }
}
