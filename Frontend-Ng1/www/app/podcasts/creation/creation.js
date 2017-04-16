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
                this.podcast.cover = podcastFetched.cover;
            });
    }

    loadTags(query) {
        return this.tagService.search(query);
    }

    changeType() {
        if (/beinsports\.fr/i.test(this.podcast.url)) {
            this.podcast.type = "BeInSports";
        } else if (/canalplus\.fr/i.test(this.podcast.url)) {
            this.podcast.type = "CanalPlus";
        } else if (/jeuxvideo\.fr/i.test(this.podcast.url)) {
            this.podcast.type = "JeuxVideoFR";
        } else if (/jeuxvideo\.com/i.test(this.podcast.url)) {
            this.podcast.type = "JeuxVideoCom";
        } else if (/parleys\.com/i.test(this.podcast.url)) {
            this.podcast.type = "Parleys";
        } else if (/pluzz\.francetv\.fr/i.test(this.podcast.url)) {
            this.podcast.type = "Pluzz";
        } else if (/youtube\.com/i.test(this.podcast.url)) {
            this.podcast.type = "Youtube";
        } else if (/dailymotion\.com/i.test(this.podcast.url)) {
            this.podcast.type = "Dailymotion";
        } else if (this.podcast.url && this.podcast.url.length > 0) {
            this.podcast.type = "RSS";
        } else {
            this.podcast.type = "upload";
        }
    }

    save() {
        this.podcastService
            .save(this.podcast)
            .then(podcast => this.$location.path('/podcasts/' + podcast.id));
    }
}

