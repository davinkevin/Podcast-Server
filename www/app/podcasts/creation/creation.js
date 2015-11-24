/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import angular from 'angular';
import {RouteConfig, View, Module, Constant} from '../../decorators';
import AppRouteConfig from '../../config/route.config';
import NgTagsInput from '../../common/modules/ngTagsInput';
import PodcastService from '../../common/service/data/podcastService';
import TypeService from '../../common/service/data/typeService';
import TagService from '../../common/service/data/tagService';
import template from './creation.html!text';

@Module({
    name : 'ps.podcasts.creation',
    modules : [
        AppRouteConfig.name,
        NgTagsInput,
        PodcastService.name,
        TypeService.name,
        TagService.name
    ]
})
@RouteConfig({
    path : '/podcast-creation',
    as : 'pac',
    resolve : {
        types : typeService => {"ngInject"; return typeService.findAll();}
    }
})
@Constant({ name : 'defaultPodcast', value : { hasToBeDeleted : true, cover : { height: 200, width: 200 }} })
@View({
    template : template
})
export default class PodcastCreationController {

    constructor($location, defaultPodcast, tagService, podcastService, types) {
        "ngInject";
        this.podcastService = podcastService;
        this.$location = $location;
        this.tagService = tagService;
        this.podcast = angular.extend(this.podcastService.getNewPodcast(), defaultPodcast );
        this.types = types;
    }

    findInfo() {
        return this.podcastService.findInfo(this.podcast.url)
            .then((podcastFetched) => {
                this.podcast.title = podcastFetched.title;
                this.podcast.description = podcastFetched.description;
                this.podcast.type = podcastFetched.type;
                this.podcast.cover.url = podcastFetched.cover.url;
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
        } else if (this.podcast.url.length > 0) {
            this.podcast.type = "RSS";
        } else {
            this.podcast.type = "Send";
        }
    }

    save() {
        this.podcastService.save(this.podcast)
            .then((podcast) => this.$location.path('/podcasts/' + podcast.id));
    }
}

