/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */

import template from './podcasts.html!text';
import './podcasts.css!';

export default class PodcastsListCtrl {
    constructor(podcasts, types) {
        "ngInject";
        this.podcasts = podcasts;
        this.types = types;
        this.filters = {
            title : '',
            type : ''
        };
    }

    static routeConfig($routeProvider, commonKey){
        "ngInject";
        $routeProvider.
            when('/podcasts', {
                template: template,
                controller: 'PodcastsListCtrl',
                controllerAs: 'plc',
                hotkeys: commonKey,
                resolve: {
                    podcasts: podcastService => {"ngInject"; return podcastService.findAll()},
                    types: typeService => {"ngInject"; return typeService.findAll() }
                }
            })
    }
}
