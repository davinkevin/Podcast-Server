'use strict';

angular.module('ps.podcast.details.upload', [
    'angularFileUpload'
])
    .directive('podcastUpload', function ($log) {
        return {
            restrcit : 'E',
            templateUrl : 'html/podcast-details-upload.html',
            scope : {
                podcast : '='
            },
            controller : 'podcastUploadCtrl'
        };
    })
    .controller('podcastUploadCtrl', function ($scope, $log) {
        $scope.onFileSelect = function($files) {
            var formData;
            angular.forEach($files, function (file) {
                formData = new FormData();
                formData.append('file', file);
                $scope.podcast.all('items')
                    .withHttpConfig({transformRequest: angular.identity})
                    .customPOST(formData, 'upload', undefined, {'Content-Type': undefined}).then(function (item) {
                        $log.info("Upload de l'item suivant");
                        $log.info(item);
                    });
            });
        };
    });
