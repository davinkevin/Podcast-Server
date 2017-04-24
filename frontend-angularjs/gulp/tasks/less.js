import gulp from 'gulp';
import util from 'gulp-util';
import less from 'gulp-less';
import paths from '../paths';

function logError(err) {
    util.log(err);
    this.emit('end');
}

gulp.task('less', () =>
        gulp.src([paths.glob.less])
            .pipe(less({
                paths: [ paths.srcDir ]
            })
                .on('error', logError))
            .pipe(gulp.dest(paths.app.app))
);