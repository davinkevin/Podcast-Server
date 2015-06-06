
class podcastUploadDirective {

    constructor() {
        this.restrcit = 'E';
        this.templateUrl = 'html/podcast-details-upload.html';
        this.scope = {
            podcast : '='
        };
        this.controller = 'podcastUploadCtrl';
        this.controllerAs = 'puc';
        this.bindToController = true;
    }
}

class podcastUploadCtrl{

    constructor($scope, itemService, $notification) {
        this.$scope = $scope;
        this.itemService = itemService;
        this.$notification = $notification;
    }

    onFileSelect($files) {
        angular.forEach($files, (file) => {
            this.itemService.upload(this.podcast, file)
                .then((item) => {
                    this.$scope.$emit("podcastEdition:upload");
                    try {
                        this.$notification('Upload effectué', {
                            body: item.title,
                            icon: item.cover.url,
                            delay: 5000
                        });
                    } catch (e) {}
                });
        });
    }
}


angular.module('ps.podcast.details.upload', [
    'angularFileUpload',
    'ps.dataService.item',
    'notification'
])
    .directive('podcastUpload', () => new podcastUploadDirective())
    .controller('podcastUploadCtrl', podcastUploadCtrl);
