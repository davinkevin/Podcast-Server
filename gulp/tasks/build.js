import gulp from "gulp";
import jspm from 'jspm';
import sourcemaps from 'gulp-sourcemaps';
import ngAnnotate from 'gulp-ng-annotate';
import uglify from 'gulp-uglify';
import rename from 'gulp-rename';
import minifyCSS from 'gulp-minify-css';
import flatten from 'gulp-flatten';
import inject from 'gulp-inject';
import runSequence from 'run-sequence';
import del from 'del';
import Builder from 'systemjs-builder';
import replace from 'gulp-replace';
import gzip from 'gulp-gzip';
import paths from '../paths';

let prodFiles = ['min.css', 'min.js'].map(ext => `${paths.release.root}/${paths.app.name}.${ext}`);
let filesToDelete = ['css', 'css.map', 'js', 'js.map'].map(ext => `${paths.release.root}/${paths.app.name}.${ext}`);

gulp.task('build:jspm', function(cb){
    new jspm.Builder().buildStatic(paths.app.entryPoint, `${paths.release.root}/${paths.app.name}.js`, {sourceMaps: true})
        .then(() => cb())
        .catch((ex) => cb(new Error(ex)));
});

gulp.task('build:js', () =>
    gulp.src(`${paths.release.root}/${paths.app.name}.js`)
        .pipe(sourcemaps.init({loadMaps: true}))
        .pipe(ngAnnotate())
        .pipe(uglify())
        .pipe(rename({suffix : '.min'}))
        .pipe(sourcemaps.write('.'))
        .pipe(gulp.dest(paths.release.root))
        .pipe(gzip())
        .pipe(gulp.dest(paths.release.root))
);

gulp.task('build:css', () =>
    gulp.src(`${paths.releaseDir}/${paths.app.name}.css`)
        .pipe(minifyCSS())
        .pipe(replace(/url\([^\)]*jspm_packages[^\)]*\/fonts\/([^\)]*)\)/g, 'url(/fonts/$1)'))
        .pipe(rename({suffix : '.min'}))
        .pipe(gulp.dest(paths.release.root))
        .pipe(gzip())
        .pipe(gulp.dest(paths.release.root))
);

gulp.task('build:index', () =>
    gulp.src(`${paths.srcDir}/index.html`)
        .pipe(inject(gulp.src(prodFiles, {read: false}), { ignorePath: paths.releaseDirName }))
        .pipe(gulp.dest(paths.release.root))
);

gulp.task('build:fonts', () =>
    gulp.src([paths.jspm.fonts, paths.glob.projectFonts, '!'+paths.glob.fonts])
        .pipe(flatten())
        .pipe(gulp.dest(paths.release.fonts))
);

gulp.task('build:pre-clean', (cb) =>
        del([`${paths.release.root}/**/*`, `!${paths.release.root}/.keep`], cb)
);

gulp.task('build:clean', (cal) =>  del(filesToDelete, cal));

gulp.task('build', (cal) => {
    runSequence(
        ['build:pre-clean'],
        'build:jspm',
        ['build:js', 'build:css', 'build:fonts'],
        'build:index',
        'build:clean',
        cal);
});