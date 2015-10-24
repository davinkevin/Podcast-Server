import gulp from "gulp";
import jspm from 'jspm';
import sourcemaps from 'gulp-sourcemaps';
import ngAnnotate from 'gulp-ng-annotate';
import uglify from 'gulp-uglify';
import rename from 'gulp-rename';
import minifyCSS from 'gulp-minify-css';
import inject from 'gulp-inject';
import runSequence from 'run-sequence';
import del from 'del';
import Builder from 'systemjs-builder';
import paths from '../paths';

gulp.task('build-jspm', function(cal){
  let builder = new Builder();
  builder.loadConfig(paths.systemConfigJs)
    .then(() => {
      builder.buildStatic(paths.app.entryPoint, `${paths.releaseDir}/${paths.app.name}.js`, {sourceMaps: true})
        .then(() => cal())
        .catch((ex) => cal(new Error(ex)));
    });
});

gulp.task('build-js', () => {
  return gulp.src(`${paths.releaseDir}/${paths.app.name}.js`)
    .pipe(sourcemaps.init({loadMaps: true}))
    .pipe(ngAnnotate())
    .pipe(uglify())
    .pipe(rename({suffix : '.min'}))
    .pipe(sourcemaps.write('.'))
    .pipe(gulp.dest(paths.releaseDir))
});

gulp.task('build-css', () => {
  return gulp.src(`${paths.releaseDir}/${paths.app.name}.css`)
    .pipe(minifyCSS())
    .pipe(rename({suffix : '.min'}))
    .pipe(gulp.dest(paths.releaseDir))
});

gulp.task('build-index-html', () => {
  let sources = gulp.src([
      `${paths.releaseDir}/${paths.app.name}.min.css`,
      `${paths.releaseDir}/${paths.app.name}.min.js`
  ], {read: false});
  return gulp.src(`${paths.srcDir}/index.html`)
    .pipe(inject(sources, { ignorePath: paths.releaseDirName }))
    .pipe(gulp.dest(paths.releaseDir))
});

gulp.task('build-clean', (cal) => {
  let targets = [
      `${paths.releaseDir}/${paths.app.name}.css`,
      `${paths.releaseDir}/${paths.app.name}.css.map`,
      `${paths.releaseDir}/${paths.app.name}.js`,
      `${paths.releaseDir}/${paths.app.name}.js.map`
  ];
  del(targets, cal);
});

gulp.task('build', (cal) => {
  runSequence(
    'build-jspm',
    ['build-js', 'build-css'],
    'build-index-html',
    'build-clean',
    cal);
});
