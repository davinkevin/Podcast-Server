import gulp from 'gulp';
import flatten from 'gulp-flatten';
import paths from '../paths';

gulp.task('fonts', () =>
        gulp.src([paths.jspm.fonts, paths.glob.projectFonts, '!'+paths.glob.fonts])
            .pipe(flatten())
            .pipe(gulp.dest(paths.app.fonts))
);