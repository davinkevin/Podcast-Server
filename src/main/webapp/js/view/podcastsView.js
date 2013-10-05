

var PodcastsView = Backbone.View.extend({
    el: '#listPodcasts',
    tagName: 'ul',

    initialize: function() {
        this.listenTo(this.collection, 'sync', this.render);
    },

    render: function(){
        $(this.el).addClass('thumbnails');
        this.collection.each(function(podcast){
            var podcastView = new PodcastView({ model: podcast});
            this.$el.append(podcastView.render().el); // calling render method manually..
        }, this);
        return this; // returning this for chaining..
    }
});