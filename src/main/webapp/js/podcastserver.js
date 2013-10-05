

$(document).ready(function() {

    $("#changeToItunes").click(function () {
        var pathname = "itpc://" + window.location.host + window.location.pathname;
        $("a").each( function(i) {
            if ($(this).attr("href").indexOf("itpc") == -1) {
                $(this).attr("href", pathname.concat($(this).attr("href")));
                $("#changeToItunes").text("Convertir en flux XML");
            } else {
                $(this).attr("href", $(this).attr("href").substring(pathname.length, $(this).attr("href").length));
                $("#changeToItunes").text("Convertir en flux iTunes");
            }
        });
    });

    $("#changeToExternal").click(function () {
        var pathname = "http://" + "kevinappart.homeip.net/podcast/" ;
        $("a").each( function(i) {
            if ($(this).attr("href").indexOf("itpc") != -1) {
                $(this).attr("href", $(this).attr("href").substring(pathname.length, $(this).attr("href").length));
            }
            if ($(this).attr("href").indexOf("kevinappart.homeip.net") == -1) {
                $(this).attr("href", pathname.concat($(this).attr("href")));
            } else {
                $(this).attr("href", $(this).attr("href").substring(pathname.length, $(this).attr("href").length));
            }
        });
    });

});