
class DownloadCtrl {

    constructor($scope, DonwloadManager, $notification) {
        this.DonwloadManager = DonwloadManager;
        this.$notification = $notification;
        this.items =[];
        this.waitingitems = [];
        this.numberOfSimDl = 0;

        this.DonwloadManager.getNumberOfSimDl().then((value) => {
            this.numberOfSimDl = parseInt(value);
        });

        /** Websocket Connection */
        this.DonwloadManager.ws
            .subscribe("/app/download", (message) => this.onSubscribeDownload(message), $scope)
            .subscribe("/app/waiting", (message) => this.onSubscribeWaiting(message), $scope)
            .subscribe("/topic/download", (message) => this.onDownloadUpdate(message), $scope)
            .subscribe("/topic/waiting", (message) => this.onWaitingUpdate(message), $scope);
    }

    onSubscribeDownload(message) {
        this.items = JSON.parse(message.body);
    }
    onSubscribeWaiting(message) {
        this.waitingitems = JSON.parse(message.body);
    }
    onDownloadUpdate(message) {
        let item = JSON.parse(message.body);
        let elemToUpdate = _.find(this.items, { 'id': item.id });
        switch (item.status) {
            case 'Started' :
            case 'Paused' :
                if (elemToUpdate)
                    _.assign(elemToUpdate, item);
                else
                    this.items.push(item);
                break;
            case 'Finish' :
                try {
                    this.$notification('Téléchargement terminé', {
                        body: item.title,
                        icon: item.cover.url,
                        delay: 5000
                    });
                } catch (e) {

                }
                this.onStoppedFromWS(elemToUpdate);
                break;
            case 'Stopped' :
                this.onStoppedFromWS(elemToUpdate);
                break;
        }
    }

    onStoppedFromWS(elemToUpdate) {
        if (elemToUpdate) {
            _.remove(this.items, function (item) {
                return item.id === elemToUpdate.id;
            });
        }
    }

    onWaitingUpdate(message) {
        let remoteWaitingItems = JSON.parse(message.body);
        _.updateinplace(this.waitingitems, remoteWaitingItems, (inArray, elem) => _.findIndex(inArray, { 'id': elem.id }), true);
    }

    getTypeFromStatus(item) {
        if (item.status === "Paused")
            return "warning";
        return "info";
    }
    updateNumberOfSimDl(number) {
        this.DonwloadManager.updateNumberOfSimDl(number);
    }

    /** Spécifique aux éléments de la liste : **/
    download(item){
        this.DonwloadManager.download(item);
    }
    stopDownload(item){
        this.DonwloadManager.ws.stop(item);
    }
    toggleDownload(item){
        this.DonwloadManager.ws.toggle(item);
    }

    /** Global **/
    stopAllDownload(){
        this.DonwloadManager.stopAllDownload();
    }
    pauseAllDownload(){
        this.DonwloadManager.pauseAllDownload();
    }
    restartAllCurrentDownload(){
        this.DonwloadManager.restartAllCurrentDownload();
    }
    removeFromQueue(item){
        this.DonwloadManager.removeFromQueue(item);
    }
    dontDonwload(item){
        this.DonwloadManager.dontDonwload(item);
    }
    moveInWaitingList(item, position){
        this.DonwloadManager.moveInWaitingList(item, position);
    }

}

angular.module('ps.download', [
    'ps.config.route',
    'ps.dataService.donwloadManager',
    'notification'
])
    .config(($routeProvider, commonKey) =>
        $routeProvider.
            when('/download', {
                templateUrl: 'html/download.html',
                controller: 'DownloadCtrl',
                controllerAs: 'dc',
                hotkeys: commonKey
            })
    )
    .controller('DownloadCtrl', DownloadCtrl);