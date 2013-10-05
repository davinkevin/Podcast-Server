
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<jsp:include page="include/head.jsp" >
    <jsp:param name="title" value="Podcast Server" />
</jsp:include>
<body>

<jsp:include page="include/header.jsp" />

<div class="container">
    <div class="row">

        <ul id="myTab" class="nav nav-tabs">
            <li class="active"><a href="#add-podcast" data-toggle="tab">Ajout Podcast</a></li>
            <li class=""><a href="#advanced-add" data-toggle="tab">Ajout avancé</a></li>
        </ul>

        <div id="myTabContent" class="tab-content">
            <div class="tab-pane fade active in" id="add-podcast">
                <div class="span4">
                    <form id="simple-addPodcastForm">
                        <fieldset>
                            <div class="controls controls-row">
                                <input class="span4" type="text" placeholder="URL" id="simple-addPodcastURLField">
                            </div>

                            <select id="simple-addPodcastTypetField">
                                <option value="rss">Rss</option>
                                <option value="canal">Canal+</option>
                                <option value="youtube">Youtube</option>
                            </select>
                        </fieldset>
                    </form>
                    <div>
                        <button id="simple-addPodcastButton" class="btn">Ajouter Podcast</button>
                    </div>
                </div>
                <div id="podcastPreview">

                </div>
            </div>
            <div class="tab-pane fade" id="advanced-add">
                <form id="addPodcastForm">
                    <fieldset>
                        <%--<legend>Ajouter un Podcast</legend>--%>
                        <div class="controls controls-row">
                            <input class="span4" type="text" placeholder="Titre" id="addPodcastTitleField">
                        </div>
                        <div class="controls controls-row">
                            <input class="span4" type="text" placeholder="URL" id="addPodcastURLField">
                        </div>
                        <div class="controls controls-row">
                            <input class="span4" type="text" placeholder="Cover URL" id="addPodcastCoverURLField" >

                    <span class="span2 controls controls-row input-append">
                        <input class="span2" type="text" placeholder="width" id="addPodcastCoverWidthField">
                        <span class="add-on">px</span>
                    </span>
                    <span class="span3 controls controls-row input-append">
                        <input class="span2" type="text" placeholder="height" id="addPodcastHeightField">
                        <span class="add-on">px</span>
                    </span>
                        </div>
                        <select id="addPodcastTypetField">
                            <option value="rss">Rss</option>
                            <option value="canalplus">Canal+</option>
                            <option value="youtube">Youtube</option>
                        </select>
                        <div>
                            <button id="addPodcastButton" class="btn">Ajouter Podcast</button>
                        </div>
                    </fieldset>
                </form>
            </div>
        </div>

    </div>
</div>


<script type="text/template" id="podcast-preview-template">
    <span class="span4"></span>
    <ul class="thumbnails">
        <li class="thumbnail">
            <img src="<@= cover.url @>" class="span2" alt="">
            <center><@= title @></center>

        </li>
    </ul>
</script>

<jsp:include page="include/libJavascript.jsp" />
<script src="../../js/model/podcast.js"></script>
<script src="../../js/view/addPodcastView.js"></script>
<script src="../../js/view/podcastPreView.js"></script>

<script>
    var podcastToPreview = null;
    $(document).ready(function () {
        console.log("Chargement");
        var addPodcastView = new AddPodcastView();

        $("#simple-addPodcastURLField").focusout(function() {
            console.log($('#simple-addPodcastURLField').val());
            $.ajax({
                type: "POST",
                url: 'api/podcast/generatePodcastFromURL',
                data: $("#simple-addPodcastURLField").val(),
                contentType:"application/json; charset=utf-8",
                dataType:"json",
                success : function (data) {
                    console.log(data);
                    podcastToPreview = new Podcast(data);
                    var podcastPreView = new PodcastPreView({ model:podcastToPreview });
                }
            });
        });

    });
</script>

</body>
</html>