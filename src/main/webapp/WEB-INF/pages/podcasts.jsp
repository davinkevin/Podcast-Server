
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
    <jsp:include page="include/head.jsp" >
        <jsp:param name="title" value="Podcast Server" />
    </jsp:include>
    <body>

    <jsp:include page="include/header.jsp" />

    <div class="container">
        <div class="row">
            <ul id="listPodcasts" class="thumbnail"></ul>
        </div>
    </div>


    <div style="visibility: hidden">
        <a class="open-Fancy" href="#fancyPodcastDetails"></a>
        <div id="fancyPodcastDetails">
        </div>
    </div>


    <!-- Template Backbone -->
    <script type="text/template" id="podcast-item-template">
        <div class="thumbnail">
            <img src="<@= cover.url @>" height="210" width="210" class="thumbnail-picture">
            <center><@= title @></center>
        </div>
    </script>

    <script type="text/template" id="podcast-details-template">
        <div class="span4 thumbnail">
            <img src="<@= cover.url @>" height="<@= cover.height @>" width="<@= cover.width @>">
            <center><@= title @></center>
            <a class="btn btn-force-refresh">
                <i class="icon-retweet"></i>
            </a>
        </div>
        <div class="span10">
            <table class="table table-hover">
                <thead>
                <tr>
                    <th>Titre</th>
                    <th>Publication</th>
                    <th class="span2"></th>
                </tr>
                </thead>
                <tbody id="tableItem">
                </tbody>
            </table>
        </div>
    </script>

    <script type="text/template" id="podcast-details-item-template" >
            <td><@= title @></td>
            <td><@= moment(pubdate).format("DD/MM/YYYY") @></td>
            <td>
                <@ if (localUrl == null) { @>
                    <a class="btn btn-download">
                        <i class="icon-download-alt"></i>
                    </a>
                    <a class="btn" href="<@= url @>">
                        <i class="icon-globe"></i>Stream
                    </a>
                <@ } else { @>
                    <a class="btn btn-play" href="<@= localUrl @>">
                        <i class="icon-play"></i>
                    </a>
                    <a class="btn btn-delete">
                        <i class="icon-trash"></i>
                    </a>
                <@ } @>
            </td>
    </script>
    <jsp:include page="include/libJavascript.jsp" />

    <!-- Backbone Element -->
    <script src="../../js/model/podcast.js"></script>
    <script src="../../js/model/item.js"></script>
    <script src="../../js/collection/podcasts.js"></script>
    <script src="../../js/view/podcastView.js"></script>
    <script src="../../js/view/podcastsView.js"></script>
    <script src="../../js/view/detailPodcastView.js"></script>
    <script src="../../js/view/detailPodcastItemView.js"></script>

    <script>
        var podcasts = new Podcasts();
        podcasts.fetch();
        var podcastsView = new PodcastsView({ collection: podcasts });

        $(".open-Fancy").fancybox({
            minWidth    : 1200,
            minHeight   : 600,
            maxHeight	: 600,
            fitToView	: false,
            width		: '70%',
            height		: '70%',
            autoSize	: true,
            closeClick	: false,
            openEffect	: 'none',
            closeEffect	: 'none'
        });
    </script>

    </body>
</html>