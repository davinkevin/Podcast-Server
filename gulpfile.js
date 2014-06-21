// Include gulp and Our Plugins
var gulp = require('gulp'),
    jshint = require('gulp-rename'),
    less = require('gulp-less'),
    minifyCSS = require('gulp-minify-css'),
    ngmin = require('gulp-ngmin'),
    concat = require('gulp-concat'),
    uglify = require('gulp-uglify'),
    rename = require('gulp-rename'),
    ngHtml2Js = require("gulp-ng-html2js"),
    addsrc = require('gulp-add-src'),
    inject = require("gulp-inject"),
    bowerFiles = require('gulp-bower-files'),
    debug = require('gulp-debug');

    // Location Files :
var jsLocation = ['src/main/webapp/app/js/*.js', '!src/main/webapp/app/js/all*.js', '!src/main/webapp/app/js/*.min.js'],
    lessLocation = 'src/main/webapp/app/less/*.less',
    htmlLocation = 'src/main/webapp/app/html/*.html',
    indexLocation = 'src/main/webapp/WEB-INF/pages/index.jsp';

var cssDestionation = 'src/main/webapp/app/css',
    jsDestination = 'src/main/webapp/app/js';

// Lint Task
gulp.task('lint', function() {
    return gulp.src(jsLocation)
        .pipe(jshint())
        .pipe(jshint.reporter('default'));
});

gulp.task('scripts', function() {
    gulp.src(htmlLocation)
    .pipe(ngHtml2Js({
        moduleName: "podcast.partial",
        prefix: "html/"
    }))
    .pipe(concat("partials.js"))
    //.pipe(gulp.dest(jsDestination))
    .pipe(addsrc(jsLocation))
    .pipe(concat('all.js'))
    .pipe(gulp.dest(jsDestination))
    .pipe(ngmin())
    .pipe(rename('all.min.js'))
    .pipe(uglify({
            mangle : false
        }))
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
    //gulp.watch(jsLocation, ['lint']);
    gulp.watch([jsLocation, htmlLocation], ['scripts']);
    gulp.watch(lessLocation, ['less']);
});

gulp.task('inject', function() {
    gulp.src(indexLocation)
        .pipe(inject(bowerFiles({read: false, debugging : false}), { ignorePath : "/bower_components/", addPrefix : "/js/lib/"}))
        .pipe(gulp.dest("src/main/webapp/WEB-INF/pages/"));
    bowerFiles({checkExistence : true, read: true, debugging : true}).pipe(gulp.dest("src/main/webapp/app/js/lib/"));
});

// Default Task
gulp.task('default', ['less', 'scripts']);