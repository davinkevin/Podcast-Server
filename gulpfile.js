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
    bowerFiles = require('gulp-bower-files'),
    debug = require('gulp-debug'),
    sourcemaps = require('gulp-sourcemaps')
    args = require('yargs').argv;

    // Location Files :
var angularAppLocation = [  'src/main/webapp/app/js/**/*.js', '!src/main/webapp/app/js/all*.js',
                            '!src/main/webapp/app/js/*.min.js', '!src/main/webapp/app/js/lib/**/*.js',
                            '!src/main/webapp/app/js/**/*.module.js'],
    angularAppModule = ['src/main/webapp/app/js/**/*.module.js'],
    lessLocation = 'src/main/webapp/app/less/*.less',
    htmlLocation = 'src/main/webapp/app/html/*.html',
    indexLocation = 'src/main/webapp/WEB-INF/pages/index.html';

var cssDestionation = 'src/main/webapp/app/css',
    jsDestination = 'src/main/webapp/app/js';

var environnement = (args.environnement == undefined) ? 'production' : args.environnement;

// Lint Task
gulp.task('lint', function() {
    return gulp.src(angularAppLocation)
        .pipe(jshint())
        .pipe(jshint.reporter('default'));
});

gulp.task('js', function() {
    gulp.src(htmlLocation)
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
        .pipe(sourcemaps.write())
        .pipe(rename('all.min.js'))
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
    gulp.watch([angularAppLocation, htmlLocation], ['scripts']);
    gulp.watch(lessLocation, ['less']);
});

gulp.task('inject', function() {
    gulp.src(indexLocation)
        .pipe(inject(bowerFiles({read: false, debugging : false, env : environnement}), { ignorePath : "/bower_components/", addPrefix : "/js/lib/"}))
        .pipe(gulp.dest("src/main/webapp/WEB-INF/pages/"));
    bowerFiles({checkExistence : true, read: true, debugging : false, env : environnement}).pipe(gulp.dest("src/main/webapp/app/js/lib/"));
});

// Default Task
gulp.task('default', ['less', 'js', 'inject']);