

var PodcastView = Backbone.View.extend({

    tagName: 'li',

    template: _.template($('#podcast-item-template').html()),

    events: {
        'click .thumbnail-picture': 'showDetails'
    },

    initialize: function() {
        this.render();
    },

    showDetails : function () {
        console.log(this.model.toJSON());
        var detailPodcastView = new DetailsPodcastView({model: this.model});
        detailPodcastView.render().el;
        $(".open-Fancy").click();
        return false;
    },

    render: function() {
        $(this.el).addClass('span3');
        this.$el.html(this.template(this.model.toJSON()));
        return this;
    }

});