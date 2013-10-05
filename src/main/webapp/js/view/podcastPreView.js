

var PodcastPreView = Backbone.View.extend({

    el:'#add-podcast',

    template: _.template($('#podcast-preview-template').html()),

    events: {
        'click #simple-addPodcastButton': 'save_podcast'
    },

    initialize: function() {
        this.render();
        //$(this.el).delegate('#simple-addPodcastButton', 'click', this.save_podcast())
    },

    save_podcast: function() {
//        $.ajax({
//            type: "POST",
//            url: 'api/podcast/generatePodcastFromURL',
//            data: $("#simple-addPodcastURLField").val(),
//            contentType:"application/json; charset=utf-8",
//            dataType:"json",
//            success : function (data) {
//                var podcastToPreview = new Podcast(data);
//                podcastToPreview.set("type", $('#simple-addPodcastTypetField').val())
//                podcastToPreview.set("id", null);
//                podcastToPreview.save();
//            }
//        });
        podcastToPreview.set("type", $('#simple-addPodcastTypetField').val())
        podcastToPreview.set("id", null);
        podcastToPreview.save();
        console.log(this.model.toJSON());
    },

    render: function() {
        this.$el.find('#podcastPreview').html(this.template(this.model.toJSON()));
        return this;
    }

});