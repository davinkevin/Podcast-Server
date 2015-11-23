/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, View, Module} from '../../../decorators';
import template from './navbar.html!text';
import './navbar.css!';

@Module({
    name : 'ps.common.component.navbar'
})
@Component({
    selector : 'navbar',
    transclude : true,
    replace : true,
    as : 'navbar'
})
@View({
    template : template
})
export default class NavbarComponent {
    constructor() {
        this.navCollapsed = true;
    }

    static link(_, element) {
        element.removeClass('hidden');
    }
}
