

var Podcast = Backbone.Model.extend({
    urlRoot: '/api/podcast',
    defaults: {
        id: null,
        title: '',
        url: '',
        signature: null,
        type: '',
        lastUpdate: null,
        items: null
    }
});
