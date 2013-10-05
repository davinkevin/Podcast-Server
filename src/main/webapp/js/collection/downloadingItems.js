
var DownloadingItems = Backbone.Collection.extend({
    url: '/task/downloadManager/downloading',
    model: Item
});