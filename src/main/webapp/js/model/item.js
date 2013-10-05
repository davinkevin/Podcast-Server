

var Item = Backbone.Model.extend({
    urlRoot: '/api/item',
    defaults: {
        id: null,
        title: '',
        url: '',
        pubdate: '',
        localUrl:null,
        status:'',
        cover: {},
        progression:null
    }
});