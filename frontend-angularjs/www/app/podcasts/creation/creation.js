/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import angular from 'angular';
import {Component, Module, Constant} from '../../decorators';
import AppRouteConfig from '../../config/route';
import NgTagsInput from '../../common/modules/ngTagsInput';
import PodcastService from '../../common/service/data/podcastService';
import TypeService from '../../common/service/data/typeService';
import TagService from '../../common/service/data/tagService';
import template from './creation.html!text';

@Module({
    name : 'ps.podcasts.creation',
    modules : [ AppRouteConfig, NgTagsInput, PodcastService, TypeService, TagService ]
})
@Component({
    selector : 'podcast-creation',
    as : 'pac',
    template : template,

    path : '/podcasts/new',
    resolve : { types: typeService => {"ngInject"; return typeService.findAll();}}
})
@Constant({ name : 'defaultPodcast', value : { hasToBeDeleted : true, cover : { height: 200, width: 200 }} })
export default class PodcastCreationController {

    constructor($location, TitleService, defaultPodcast, tagService, podcastService) {
        "ngInject";
        this.podcastService = podcastService;
        this.$location = $location;
        this.tagService = tagService;
        this.podcast = angular.extend({}, defaultPodcast);
        TitleService.title = 'Creation';
    }

    findInfo() {
        return this.podcastService
            .findInfo(this.podcast.url)
            .then(podcastFetched => {
                this.podcast.title = podcastFetched.title;
                this.podcast.description = podcastFetched.description;
                this.podcast.type = podcastFetched.type;
                this.podcast.url = podcastFetched.url;
                this.podcast.cover = podcastFetched.cover;
            });
    }

    loadTags(query) {
        return this.tagService.search(query);
    }

    save() {
        this.podcastService
            .save(this.podcast)
            .then(podcast => this.$location.path('/podcasts/' + podcast.id));
    }
}

