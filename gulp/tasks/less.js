import gulp from 'gulp';
import less from 'gulp-less';
import paths from '../paths';

gulp.task('less', () =>
    gulp.src(['./public/**/*.less'])
      .pipe(less({
            paths: [ paths.root ]
        }))
      .pipe(gulp.dest('./public'))
);