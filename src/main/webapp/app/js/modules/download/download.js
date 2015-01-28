angular.module('ps.download', [
    'ps.config.route',
    'ps.dataService.donwloadManager',
    'notification',
    'AngularStompDK'
])
    .config(function($routeProvider, commonKey) {
        $routeProvider.
            when('/download', {
                templateUrl: 'html/download.html',
                controller: 'DownloadCtrl',
                hotkeys: commonKey
            })
    })
    .controller('DownloadCtrl', function ($scope, ngstomp, DonwloadManager, Notification) {
        //$scope.items = DonwloadManager.getDownloading().$object;
        $scope.waitingitems = [];

        DonwloadManager.getNumberOfSimDl().then(function (data) {
            $scope.numberOfSimDl = parseInt(data);
        });

        $scope.getTypeFromStatus = function (item) {
            if (item.status === "Paused")
                return "warning";
            return "info";
        };

        $scope.updateNumberOfSimDl = DonwloadManager.updateNumberOfSimDl;

        /** Spécifique aux éléments de la liste : **/
        $scope.download = DonwloadManager.download;
        $scope.stopDownload = DonwloadManager.stopDownload;
        $scope.toggleDownload = DonwloadManager.toggleDownload;

        /** Global **/
        $scope.stopAllDownload = DonwloadManager.stopAllDownload;
        $scope.pauseAllDownload = DonwloadManager.pauseAllDownload;
        $scope.restartAllCurrentDownload = DonwloadManager.restartAllCurrentDownload;
        $scope.removeFromQueue = DonwloadManager.removeFromQueue;
        $scope.dontDonwload = DonwloadManager.dontDonwload;


        /** Websocket Connection */
        ngstomp
            .subscribe("/app/download", function(message) {
                $scope.items = JSON.parse(message.body);
            }, $scope)
            .subscribe("/app/waiting", function (message) {
                $scope.waitingitems = JSON.parse(message.body);
            }, $scope)
            .subscribe("/topic/download", function (message) {
                var item = JSON.parse(message.body);
                var elemToUpdate = _.find($scope.items, { 'id': item.id });
                switch (item.status) {
                    case 'Started' :
                    case 'Paused' :
                        if (elemToUpdate)
                            _.assign(elemToUpdate, item);
                        else
                            $scope.items.push(item);
                        break;
                    case 'Finish' :
                        new Notification('Téléchargement terminé', {
                            body: item.title,
                            icon: item.cover.url,
                            delay: 5000
                        });
                    case 'Stopped' :
                        if (elemToUpdate){
                            _.remove($scope.items, function (item) {
                                return item.id === elemToUpdate.id;
                            });
                        }
                        break;
                }
        }, $scope)
            .subscribe("/topic/waiting", function (message) {
                var remoteWaitingItems = JSON.parse(message.body);
                _.updateinplace($scope.waitingitems, remoteWaitingItems, function(inArray, elem) {
                    return _.findIndex(inArray, { 'id': elem.id });
                });
            }, $scope);

    });