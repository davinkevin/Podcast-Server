
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<jsp:include page="include/head.jsp" >
    <jsp:param name="title" value="Podcast Server" />
</jsp:include>
<body>

<jsp:include page="include/header.jsp" />

<div class="container downloadList-btn">
    <div class="pull-right">
        <a class="btn btn-play_all"><i class="icon-play"></i>Demarrer</a>
        <a class="btn btn-pause_all"><i class="icon-pause"></i>Pause</a>
        <a class="btn btn-stop_all"><i class="icon-stop"></i>Stop</a>
    </div>
</div>

<div id="downloadList" class="container downloadlist">


</div>

<!-- Template Backbone -->
<script type="text/template" id="downloading-item-template">
    <div class="row">
        <span class="span1">
            <img src="<@= cover.url @>" />
        </span>
        <span class="span10"><@= title @></span>
        <span class="span9">
            <div class="progress progress-striped active">
                <div class="bar" style="width: <@= progression @>%;"><@= progression @> %</div>
            </div>
        </span>
        <span class="span2">

                <%--<a class="btn btn-pause"><i class="icon-pause"></i></a>--%>
                <a class="btn btn-stop pull-right"><i class="icon-stop"></i></a>
                <a class="btn btn-play_pause pull-right"><i class="icon-play"></i><i class="icon-pause"></i></a>
                <%--<a class="btn btn-reload"><i class="iconic-reload-alt"></i></a>--%>
        </span>
    </div>
</script>

<jsp:include page="include/libJavascript.jsp" />
<script src="../../js/model/item.js"></script>
<script src="../../js/collection/downloadingItems.js"></script>
<script src="../../js/view/downloadingItemView.js"></script>
<script src="../../js/view/downloadingItemsView.js"></script>

<script>
    var items = new DownloadingItems();
    items.fetch();

    var downloadingItemsView = new DownloadingItemsView({ collection: items });
    setInterval(downloadingItemsView.reload, 3000);

    $('.btn-play_all').click(function() {
        $.ajax({
            type: "GET",
            url: 'task/downloadManager/restartAllDownload',
        });
    });

    $('.btn-pause_all').click(function() {
        $.ajax({
            type: "GET",
            url: 'task/downloadManager/pauseAllDownload',
        });
    });

    $('.btn-stop_all').click(function() {
        $.ajax({
            type: "GET",
            url: 'task/downloadManager/stopAllDownload',
        });
    });

</script>

</body>
</html>