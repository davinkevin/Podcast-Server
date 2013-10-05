var DownloadingItemsView = Backbone.View.extend({

    el: '#downloadList',

    initialize: function() {
        this.listenTo(this.collection, 'sync', this.render);
        _.bindAll( this, 'render', 'reload');
    },

    reload: function() {
        this.collection.fetch();
        this.render();
    },

    render: function(){
        this.$el.empty();
        this.collection.each(function(item){
            var downloadingItemView = new DownloadingItemView({ model: item});
            this.$el.append(downloadingItemView.render().el); // calling render method manually..
        }, this);
        return this; // returning this for chaining..
    }

});