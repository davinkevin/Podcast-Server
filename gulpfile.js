// Include gulp and Our Plugins
var gulp = require('gulp'),
    jshint = require('gulp-jshint'),
    less = require('gulp-less'),
    minifyCSS = require('gulp-minify-css'),
    ngAnnotate = require('gulp-ng-annotate'),
    concat = require('gulp-concat'),
    uglify = require('gulp-uglify'),
    rename = require('gulp-rename'),
    ngHtml2Js = require("gulp-ng-html2js"),
    inject = require("gulp-inject"),
    bowerFiles = require('main-bower-files'),
    debug = require('gulp-debug'),
    sourcemaps = require('gulp-sourcemaps'),
    args = require('yargs').argv,
    plumber = require('gulp-plumber'),
    es = require('event-stream'),
    babel = require('gulp-babel'),
    wrap = require('gulp-wrap'),
    connect = require('gulp-connect'),
    proxy = require('proxy-middleware'),
    urlparser = require('url');

var fileAppLocation = 'src/main/resources/static/app/';

// Location Files :
var angularAppLocation = [  fileAppLocation.concat('js/**/*.js'), '!'.concat(fileAppLocation).concat('js/all*.js'),
        '!'.concat(fileAppLocation).concat('js/*.min.js'), '!'.concat(fileAppLocation).concat('js/lib/**/*.js'),
        '!'.concat(fileAppLocation).concat('js/**/*.module.js')],
    angularAppModule = [fileAppLocation.concat('js/**/*.module.js')],
    lessLocation = fileAppLocation.concat('less/*.less'),
    htmlLocation = fileAppLocation.concat('html/*.html'),
    indexLocation = 'src/main/resources/static/index.html';

var cssDestionation = fileAppLocation.concat('css'),
    jsDestination = fileAppLocation.concat('js');

var env = args.env  || 'prod';

// Lint Task
gulp.task('lint', function() {
    return gulp.src(angularAppLocation)
        .pipe(jshint())
        .pipe(jshint.reporter('default'));
});

gulp.task('js', function() {
    es.merge(
        gulp.src(htmlLocation).pipe(plumber()).pipe(ngHtml2Js({ moduleName: "ps.partial", prefix: "html/" })).pipe(concat("partials.js")),
        gulp.src(angularAppModule),
        gulp.src(angularAppLocation)
    )
        .pipe(sourcemaps.init())
        .pipe(concat('all.js'))
        .pipe(babel())
        .pipe(wrap('(function(){\n<%= contents %>\n})();'))
        .pipe(ngAnnotate())
        .pipe(gulp.dest(jsDestination))
        .pipe(uglify())
        .pipe(rename('all.min.js'))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(jsDestination))
        .pipe(connect.reload());
});

gulp.task('less', function () {
    gulp.src(lessLocation)
        .pipe(less())
        .pipe(concat('podcastserver.css'))
        .pipe(minifyCSS({keepBreaks: true}))
        .pipe(gulp.dest(cssDestionation))
        .pipe(connect.reload());
});

// Watch Files For Changes
gulp.task('watch', function() {
    //gulp.watch(angularAppLocation, ['lint']);
    gulp.start("js", "less", 'inject');
    gulp.watch([angularAppLocation, htmlLocation], ['js', 'lint']);
    gulp.watch(lessLocation, ['less']);
    gulp.start('webserver')
});

gulp.task('inject', function() {
    gulp.src(indexLocation)
        .pipe(inject(gulp.src(bowerFiles({read: false, debugging : false, env : env})), { addRootSlash : false, ignorePath : "/bower_components/", addPrefix : "app/js/lib"}))
        .pipe(gulp.dest("src/main/resources/static/"));

    gulp.src(bowerFiles({checkExistence : true, read: true, debugging : false, env : env}), {base: 'bower_components'})
        .pipe(gulp.dest("src/main/resources/static/app/js/lib/"));
});

gulp.task('webserver', function() {

    function redirect(from) {
        return {
            from : from,
            to : function(remoteUrl) {
                var options = urlparser.parse(remoteUrl);
                options.route = this.from;
                options.preserveHost = true;
                return proxy(options);
            }
        }

    }

    var port = parseInt(args.port) || 8000;

    connect.server({
        root: 'src/main/resources/static/',
        port: port,
        livereload: true,
        middleware: function() {
            return [
                redirect('/api').to('http://localhost:8080/api'),
                redirect('/ws').to('http://localhost:8080/ws')
            ];
        }
    });
});

// Default Task
gulp.task('default', ['less', 'js', 'inject']);