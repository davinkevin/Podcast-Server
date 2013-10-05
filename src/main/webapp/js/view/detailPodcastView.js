

var DetailsPodcastView = Backbone.View.extend({
    el: '#fancyPodcastDetails',

    template: _.template($('#podcast-details-template').html()),

    events: {
        'click a.btn-force-refresh': 'refresh_podcast'
    },

    initialize: function() {
        this.listenTo(this.model, 'sync', this.render);
    },

    refresh_podcast: function() {
        $.ajax({
            type: "POST",
            url: '/task/updateManager/updatePodcast/force',
            data: this.model.get("id").toString(),
            contentType:"application/json; charset=utf-8",
            dataType:"text"
        });
    },

    render: function() {
        this.$el.html(this.template(this.model.toJSON()));
        _.each(this.model.get("items"), function (item) {
            //var currentItem = new Item(item);
            var detailPodcastItemView = new DetailPodcastItemView({ model: new Item(item) });
            $("#tableItem").append(detailPodcastItemView.render().el); // calling render method manually.
        });

        return this;
    }

});