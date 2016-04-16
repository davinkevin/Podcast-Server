/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */
import {Component, Module} from '../../../decorators';
import angular from 'angular';
import NgFileUpload from 'ng-file-upload';
import AngularNotification from '../../../common/modules/angularNotification';
import ItemService from '../../../common/service/data/itemService';
import template from './upload.html!text';

@Module({
    name : 'ps.podcasts.details.upload',
    modules : [ NgFileUpload, AngularNotification, ItemService ]
})
@Component({
    selector : 'podcast-upload',
    as : 'puc',
    bindings : { podcast : '='},
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
        angular.forEach($files, file => this.$uploadFile(file));
    }

    $uploadFile(file) {
        return this.itemService.upload(this.podcast, file)
            .then(item => { this.$scope.$emit("podcastEdition:upload"); return item; })
            .then(item => this.$notification('Upload done', { body: item.title, icon: item.cover.url, delay: 5000}));
    }
}

