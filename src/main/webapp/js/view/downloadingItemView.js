
var DownloadingItemView = Backbone.View.extend({


    template: _.template($('#downloading-item-template').html()),

    events: {
        'click a.btn-play_pause': 'play_pause',
        'click a.btn-pause': 'pause',
        'click a.btn-stop': 'stop',
        'click a.btn-reload': 'reload'
    },

    initialize: function() {
        this.render();
    },

    play_pause : function() {
        console.log("resume - " + this.model.get("id"));
        $.ajax({
            type: "POST",
            url: 'task/downloadManager/toogleDownload',
            data: this.model.get("id").toString(),
            contentType:"application/json; charset=utf-8",
            dataType:"text"
        });
    },

    pause : function() {
        console.log("pause");
        $.ajax({
            type: "POST",
            url: 'task/downloadManager/pauseDownload',
            data: this.model.get("id").toString(),
            contentType:"application/json; charset=utf-8",
            dataType:"text"
        });
    },

    stop : function() {
        console.log("stop");
        $.ajax({
            type: "POST",
            url: 'task/downloadManager/stopDownload',
            data: this.model.get("id").toString(),
            contentType:"application/json; charset=utf-8",
            dataType:"text"
        });
    },

    reload : function() {
        console.log("reload");
        var view = this;
        $.ajax({
            type: "GET",
            url: 'task/downloadManager/downloading/' + this.model.get("id"),
            contentType:"application/json; charset=utf-8",
            dataType:"text",
            success : function(data) {
                console.log(data);
                console.log(data.progression);
                view.model.set("progression", 30);
                view.render();
            }
        });
    },

    render: function() {
        //$(this.el).addClass('span3');
        this.$el.html(this.template(this.model.toJSON()));
        return this;
    }

});