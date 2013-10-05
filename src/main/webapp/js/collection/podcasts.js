
var Podcasts = Backbone.Collection.extend({
    url: '/api/podcast',
    model: Podcast
});