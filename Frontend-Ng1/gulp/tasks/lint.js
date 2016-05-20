import gulp from 'gulp';
import paths from '../paths';
import eslint from 'gulp-eslint';

gulp.task('lint-js', () => {
  return gulp.src([paths.glob.js])
    .pipe(eslint())
    .pipe(eslint.format());
});
