

var DetailPodcastItemView = Backbone.View.extend({
    //el: '#tableItem',

    tagName : 'tr',

    template: _.template($('#podcast-details-item-template').html()),

    events: {
        'click a.btn-download': 'add_download',
        'click a.btn-delete': 'delete_download'
    },

    initialize: function() {
        this.listenTo(this.model, 'sync', this.render);
    },

    add_download : function () {
        console.log(this.model.get("id").toString());
        $.ajax({
            type: "POST",
            url: 'task/downloadManager/queue/add',
            data: this.model.get("id").toString(),
            contentType:"application/json; charset=utf-8",
            dataType:"text"
        });
    },

    delete_download : function() {
        console.log("Delete : " + this.model.get("id").toString());
        this.model.destroy();
    },


    render: function() {
        this.$el.empty().append(this.template(this.model.toJSON()));
        return this;
    }

});