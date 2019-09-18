/**
 * Created by kevin on 01/11/2015 for Podcast Server
 */

import gulp from 'gulp';
import paths from '../paths';
import sourcemaps from "gulp-sourcemaps";
import ngAnnotate from "gulp-ng-annotate";
import rename from "gulp-rename";
import runSequence from "run-sequence";
import replace from "gulp-replace";
import flatten from "gulp-flatten";

gulp.task('skaffold',['skaffold:init'], () => {
    gulp.watch(paths.glob.less,                       ['skaffold:css:compile']);
    gulp.watch(paths.glob.js,                         ['skaffold:js:watch']);
    gulp.watch([paths.jspm.fonts, paths.glob.fonts],  ['skaffold:fonts']);
    gulp.watch(paths.glob.html,                       ['build:index']);
});

gulp.task('skaffold:js:compile', () =>
    gulp.src(`${paths.release.root}/${paths.app.name}.js`)
        .pipe(sourcemaps.init({loadMaps: true}))
        .pipe(ngAnnotate())
        .pipe(rename({suffix : '.min'}))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(paths.release.root))
);

gulp.task('skaffold:css:compile', () =>
    gulp.src(`${paths.releaseDir}/${paths.app.name}.css`)
        .pipe(replace(/url\([^\)]*jspm_packages[^\)]*\/fonts\/([^\)]*)\)/g, 'url(/fonts/$1)'))
        .pipe(rename({suffix : '.min'}))
        .pipe(gulp.dest(paths.release.root))
);

gulp.task('skaffold:fonts', () =>
    gulp.src([paths.jspm.fonts, paths.glob.projectFonts, '!'+paths.glob.fonts])
        .pipe(flatten())
        .pipe(gulp.dest(paths.release.fonts))
);

gulp.task('skaffold:js:watch', (cal) => { runSequence('build:jspm', 'skaffold:js:compile', cal); });
gulp.task('skaffold:css:watch', (cal) => { runSequence('build:jspm', 'skaffold:css:compile', cal); });
gulp.task('docker:copy', () => gulp.src(`${paths.root}/docker/*`).pipe(gulp.dest(`${paths.target}`)));


const build = (cal) => runSequence(
    'build:create-folder',
    'build:pre-clean',
    ['build:jspm', 'docker:copy'],
    ['skaffold:js:compile', 'skaffold:css:compile', 'skaffold:fonts'],
    'build:index',
    'build:clean',
    cal);

gulp.task('skaffold:init',  build);
gulp.task('skaffold:build', build);

