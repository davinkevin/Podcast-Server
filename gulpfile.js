// Include gulp and Our Plugins
var gulp = require('gulp'), jshint = require('gulp-rename'), less = require('gulp-less'), minifyCSS = require('gulp-minify-css');;

// Location Files :
var jsLocation = 'src/main/webapp/app/js/*.js',
    lessLocation = 'src/main/webapp/app/less/*.less';

var cssDestionation = 'src/main/webapp/app/css';

// Lint Task
gulp.task('lint', function() {
    return gulp.src(jsLocation)
        .pipe(jshint())
        .pipe(jshint.reporter('default'));
});


gulp.task('less', function () {
    gulp.src(lessLocation)
        .pipe(less())
        .pipe(minifyCSS({keepBreaks: true}))
        .pipe(gulp.dest(cssDestionation));
});

// Watch Files For Changes
gulp.task('watch', function() {
    gulp.watch(jsLocation, ['lint']);
    gulp.watch(lessLocation, ['less']);
});

// Default Task
gulp.task('default', ['lint', 'less', 'watch']);