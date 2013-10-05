

var ItemsView= Backbone.View.extend({

    el: '#listItem',
    tagName: 'ul',

    template: _.template($('#item-pagination-template').html()),

    events: {
        'click a.next': 'next',
        'click a.prev': 'prev',
        //'click a.orderUpdate': 'updateSortBy',
        'click a.last': 'last',
        'click a.page': 'page',
        'click a.first': 'first',
        'click a.serverpage': 'gotoPage',
        'click .serverhowmany a': 'changeCount'
    },

    initialize: function() {
        this.listenTo(this.collection, 'sync', this.render);
    },

    next: function (e) {
        e.preventDefault();
        if (this.collection.information.currentPage != this.collection.information.lastPage)
            this.collection.requestNextPage();
    },

    prev: function (e) {
        e.preventDefault();
        if (this.collection.information.currentPage != this.collection.information.firstPage)
            this.collection.requestPreviousPage();
    },

    first: function (e) {
        e.preventDefault();
        this.collection.goTo(this.collection.information.firstPage);
    },

    last: function (e) {
        e.preventDefault();
        this.collection.goTo(this.collection.information.lastPage);
    },

    page: function (e) {
        e.preventDefault();
        var page = $(e.target).text();
        this.collection.goTo(page);
    },

    render: function(){
        this.$el.find('.thumbnails').empty();
        this.collection.each(function(item){
            var itemView = new ItemView({ model: item});
            this.$el.find('.thumbnails').append(itemView.render().el); // calling render method manually..
        }, this);

        this.$el.find('.pagination').empty().html(this.template(this.collection.info()));

        return this; // returning this for chaining..
    }
});