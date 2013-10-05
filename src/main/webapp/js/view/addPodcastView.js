

var AddPodcastView = Backbone.View.extend({

    el: '#addPodcastForm',

    events: {
        'click #addPodcastButton': 'addPodcast'
    },

    addPodcast: function() {
        var podcast = new Podcast({
                                title: this.$('#addPodcastTitleField').val(),
                                url: this.$('#addPodcastURLField').val(),
                                type: this.$('#addPodcastTypetField').val(),
                                cover : {
                                    URL: this.$('#addPodcastCoverURLField').val(),
                                    width : this.$('#addPodcastCoverWidthField').val(),
                                    height : this.$('#addPodcastHeightField').val()
                                }
        });
        console.log(podcast.toJSON());
        podcast.save();
        return false;
    }
});