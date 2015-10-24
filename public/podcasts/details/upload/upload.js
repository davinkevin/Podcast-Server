
import angular from 'angular';
import PodcastUploadComponent from './upload.component.js'
import NgFileUpload from 'ng-file-upload';
import AngularNotification from 'config/angularNotification';
import ItemService from 'common/service/data/itemService';

export default angular.module('ps.podcasts.details.upload', [
    NgFileUpload,
    AngularNotification.name,
    ItemService.name
])
    .directive('podcastUpload', PodcastUploadComponent.component)
    .controller('podcastUploadCtrl', PodcastUploadComponent);
