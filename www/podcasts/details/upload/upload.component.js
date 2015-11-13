/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import angular from 'angular';
import {Component, View} from '../../../decorators';
import template from './upload.html!text';

@Component({
    selector : 'podcast-upload',
    bindToController : {
        podcast : '='
    },
    as : 'puc'
})
@View({
    template : template
})
export default class PodcastUploadComponent{

    constructor($scope, itemService, $notification) {
        "ngInject";
        this.$scope = $scope;
        this.itemService = itemService;
        this.$notification = $notification;
    }

    onFileSelect($files) {
        angular.forEach($files, (file) => {
            this.itemService.upload(this.podcast, file)
                .then((item) => {
                    this.$scope.$emit("podcastEdition:upload");
                    this.$notification('Upload effectué', {
                        body: item.title,
                        icon: item.cover.url,
                        delay: 5000
                    });
                });
        });
    }
}

