import gulp from 'gulp';
import util from 'gulp-util';
import less from 'gulp-less';
import paths from '../paths';

function logError(err) {
    util.log(err);
    this.emit('end');
}

gulp.task('less', () =>
        gulp.src(['./www/**/*.less'])
            .pipe(less({
                paths: [ paths.root ]
            })
                .on('error', logError))
            .pipe(gulp.dest('./www'))
);