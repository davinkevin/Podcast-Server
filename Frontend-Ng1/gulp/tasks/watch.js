/**
    * Created by kevin on 01/11/2015 for Podcast Server
    */

import gulp from 'gulp';
import browserSync from 'browser-sync';
import paths from '../paths';

gulp.task('watch',['less', 'fonts', 'lint-js'], () => {
    gulp.watch(paths.glob.less,                       ['less',    browserSync.reload ]);
    gulp.watch(paths.glob.js,                         ['lint-js', browserSync.reload ]);
    gulp.watch([paths.jspm.fonts, paths.glob.fonts],  ['fonts',   browserSync.reload ]);
    gulp.watch(paths.glob.html,                                   browserSync.reload );
});