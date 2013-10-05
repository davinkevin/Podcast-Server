

var ItemView = Backbone.View.extend({

    tagName: 'li',

    template: _.template($('#item-template').html()),

    events: {
        //'click .thumbnail-picture': 'showDetails'
    },

    initialize: function() {
        this.render();
    },

    render: function() {
        $(this.el).addClass('span4');
        this.$el.html(this.template(this.model.toJSON()));
        return this;
    }

});