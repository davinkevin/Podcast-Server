/**
    * Created by kevin on 25/10/2015 for PodcastServer
    */

import './navbar.css!'
import template from './navbar.html!text';

export default class NavbarComponent {
    constructor() {
        this.navCollapsed = true;
    }
    
    static component() {
        return {
            transclude : true,
            replace : true,
            restrict : 'E',
            scope : true,
            template : template,
            controller : 'navBarController',
            controllerAs : 'navbar',
            link : (scope, element) => element.removeClass('hidden')
        }
    }
}
