// Include gulp and Our Plugins
var gulp = require('gulp'),
    jshint = require('gulp-jshint'),
    less = require('gulp-less'),
    minifyCSS = require('gulp-minify-css'),
    ngAnnotate = require('gulp-ng-annotate'),
    concat = require('gulp-concat'),
    uglify = require('gulp-uglify'),
    rename = require('gulp-rename'),
    templateCache = require('gulp-angular-templatecache'),
    inject = require("gulp-inject"),
    bowerFiles = require('main-bower-files'),
    debug = require('gulp-debug'),
    sourcemaps = require('gulp-sourcemaps'),
    args = require('yargs').argv,
    plumber = require('gulp-plumber'),
    es = require('event-stream'),
    babel = require('gulp-babel'),
    wrap = require("gulp-wrap-js"),
    connect = require('gulp-connect'),
    proxy = require('proxy-middleware'),
    urlparser = require('url'),
    cachebust = require('gulp-cache-bust');

const   staticLocation = 'src/main/resources/static/',
        appLocation = staticLocation + 'app/',
        bowerComponeentsFiles = 'bower_components/**/*';

const   angularAppLocation =  appLocation + 'js/modules/**/*.js',
        lessLocation = appLocation + 'less/*.less',
        lessMainLocation = appLocation + 'less/podcastserver.less',
        htmlLocation = appLocation + 'html/*.html',
        indexLocation = staticLocation + 'index.html';

const   cssDestionation = appLocation + 'css/',
        jsDestination = appLocation + 'js/';

const   appName = 'podcastserver',
        appJsFileName = appName + '.js',
        appCssFileName = appName + '.css',
        appFiles = [cssDestionation + appCssFileName, jsDestination + appJsFileName];

var env = args.env  || 'prod';

// Lint Task
gulp.task('lint', function() {
    return gulp.src(angularAppLocation)
        .pipe(jshint())
        .pipe(jshint.reporter('default'));
});

gulp.task('js', function() {
    return es.merge(
            gulp.src(htmlLocation).pipe(plumber()).pipe(templateCache('partial.js',{ standalone : true, module: "ps.partial", root: "html/" })).pipe(concat("partials.js")),
            gulp.src(angularAppLocation)
        )
        .pipe(concat(appJsFileName))
        .pipe(sourcemaps.init())
        .pipe(babel())
        .pipe(wrap('(function() {%= body %})()'))
        .pipe(ngAnnotate())
        .pipe(gulp.dest(jsDestination))
        .pipe(uglify())
        .pipe(rename({suffix : '.min'}))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(jsDestination))
        .pipe(connect.reload());
});

gulp.task('less', function () {
    return gulp.src(lessMainLocation)
        .pipe(plumber())
        .pipe(less())
        .pipe(concat(appCssFileName))
        .pipe(gulp.dest(cssDestionation))
        .pipe(minifyCSS({keepBreaks: true}))
        .pipe(rename({suffix : '.min'}))
        .pipe(gulp.dest(cssDestionation))
        .pipe(connect.reload());
});

// Watch Files For Changes
gulp.task('watch', function() {
    gulp.start('js', 'less', 'inject', 'webserver');
    gulp.watch([angularAppLocation, htmlLocation], ['js', 'lint']);
    gulp.watch(lessLocation, ['less']);
    gulp.watch(bowerComponeentsFiles , ['inject']);
});

gulp.task('inject', function() {

    var min = (env === 'dev') ? '' : '.min';

    gulp.src(indexLocation)
        .pipe(inject(gulp.src(bowerFiles({read: false, debugging : false, env : env})), { addRootSlash : false, ignorePath : "/bower_components/", addPrefix : "app/js/lib"}))
        .pipe(inject(gulp.src(appFiles).pipe(rename({suffix : min})), {addRootSlash : false, ignorePath : staticLocation, name: 'app'}))
        .pipe(cachebust({ type : 'MD5'}))
        .pipe(gulp.dest(staticLocation))
        .pipe(connect.reload());

    gulp.src(bowerFiles({checkExistence : true, read: true, debugging : false, env : env}), {base: 'bower_components'})
        .pipe(gulp.dest(appLocation + 'js/lib/'));
});

gulp.task('webserver', function() {

    function redirect(route) {
        return {
            to : function(remoteUrl) {
                var options = urlparser.parse(remoteUrl);
                options.route = route;
                options.preserveHost = true;
                return proxy(options);
            }
        }

    }

    var port = parseInt(args.port) || 8000;

    connect.server({
        root: staticLocation,
        port: port,
        livereload: true,
        fallback: indexLocation,
        middleware: function() {
            return [
                redirect('/api').to('http://localhost:8080/api'),
                redirect('/ws').to('http://localhost:8080/ws')
            ];
        }
    });
});

// Default Task
gulp.task('default', ['lint', 'less', 'js', 'inject']);