// Include gulp and Our Plugins
var gulp = require('gulp'),
    jshint = require('gulp-rename'),
    less = require('gulp-less'),
    minifyCSS = require('gulp-minify-css'),
    ngAnnotate = require('gulp-ng-annotate'),
    concat = require('gulp-concat'),
    uglify = require('gulp-uglify'),
    rename = require('gulp-rename'),
    ngHtml2Js = require("gulp-ng-html2js"),
    addsrc = require('gulp-add-src'),
    inject = require("gulp-inject"),
    bowerFiles = require('main-bower-files'),
    debug = require('gulp-debug'),
    sourcemaps = require('gulp-sourcemaps'),
    args = require('yargs').argv,
    plumber = require('gulp-plumber');

var fileAppLocation = 'src/main/webapp/app/';

    // Location Files :
var angularAppLocation = [  fileAppLocation.concat('js/**/*.js'), '!'.concat(fileAppLocation).concat('js/all*.js'),
                            '!'.concat(fileAppLocation).concat('js/*.min.js'), '!'.concat(fileAppLocation).concat('js/lib/**/*.js'),
                            '!'.concat(fileAppLocation).concat('js/**/*.module.js')],
    angularAppModule = [fileAppLocation.concat('js/**/*.module.js')],
    lessLocation = fileAppLocation.concat('less/*.less'),
    htmlLocation = fileAppLocation.concat('html/*.html'),
    indexLocation = 'src/main/webapp/WEB-INF/pages/index.jsp';

var cssDestionation = fileAppLocation.concat('css'),
    jsDestination = fileAppLocation.concat('js');

var environnement = (args.environnement == undefined) ? 'production' : args.environnement;

// Lint Task
gulp.task('lint', function() {
    return gulp.src(angularAppLocation)
        .pipe(jshint())
        .pipe(jshint.reporter('default'));
});

gulp.task('js', function() {
    gulp.src(htmlLocation)
        .pipe(plumber())
        .pipe(ngHtml2Js({
            moduleName: "podcast.partial",
            prefix: "html/"
        }))
        .pipe(concat("partials.js"))
        //.pipe(gulp.dest(jsDestination))
        .pipe(addsrc(angularAppModule))
        .pipe(addsrc(angularAppLocation))
        .pipe(sourcemaps.init())
        .pipe(concat('all.js'))
        .pipe(gulp.dest(jsDestination))
        .pipe(ngAnnotate())
        .pipe(uglify())
        .pipe(rename('all.min.js'))
        //.pipe(sourcemaps.write())
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(jsDestination));
});

gulp.task('less', function () {
    gulp.src(lessLocation)
        .pipe(less())
        .pipe(minifyCSS({keepBreaks: true}))
        .pipe(gulp.dest(cssDestionation));
});

// Watch Files For Changes
gulp.task('watch', function() {
    //gulp.watch(angularAppLocation, ['lint']);
    gulp.start("js", "less");
    gulp.watch([angularAppLocation, htmlLocation], ['js']);
    gulp.watch(lessLocation, ['less']);
});

gulp.task('inject', function() {
    gulp.src(indexLocation)
        .pipe(inject(gulp.src(bowerFiles({read: false, debugging : false, env : environnement})), { ignorePath : "/bower_components/", addPrefix : "/js/lib/"}))
        .pipe(gulp.dest("src/main/webapp/WEB-INF/pages/"));

    gulp.src(bowerFiles({checkExistence : true, read: true, debugging : false, env : environnement}))
        .pipe(gulp.dest("src/main/webapp/app/js/lib/"));
});

// Default Task
gulp.task('default', ['less', 'js', 'inject']);