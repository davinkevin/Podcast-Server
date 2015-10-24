/**
 * Created by kevin on 25/10/2015 for PodcastServer
 */

export default class PodcastUploadComponent{

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

    static component() {
        return {
            restrict : 'E',
            templateUrl : 'podcasts/details/upload/upload.html',
            scope : {
                podcast : ':'
            },
            controller : 'podcastUploadCtrl',
            controllerAs : 'puc',
            bindToController : true
        }
    }
}

