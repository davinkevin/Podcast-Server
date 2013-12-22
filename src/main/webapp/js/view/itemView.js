

var ItemView = Backbone.View.extend({

    tagName: 'li',

    template: _.template($('#item-template').html()),

    events: {
        'click .btn-download': 'add_download'
    },

    initialize: function() {
        this.render();
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

    render: function() {
        $(this.el).addClass('span4');
        this.$el.html(this.template(this.model.toJSON()));
        return this;
    }

});