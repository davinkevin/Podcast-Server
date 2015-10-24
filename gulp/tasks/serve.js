import gulp from 'gulp';
import util from 'gulp-util';
import browserSync from 'browser-sync';
import runSequence from 'run-sequence';
import modRewrite  from 'connect-modrewrite';
import paths from '../paths';

function startBrowserSync(directoryBase, files) {
  files = files === undefined ? 'default' : files;

  browserSync({
    files: files,
    open: true,
    port: 8000,
    notify: true,
    server: {
      baseDir: directoryBase,
      middleware: [
        modRewrite(['!\\.\\w+$ /index.html [L]']) // require for HTML5 mode
      ]
    },
    browser: 'default'
  });
}

gulp.task('serve', ['less'], () => {

  startBrowserSync([paths.srcDir, './' ]);

  gulp.watch(`${paths.glob.less}`, ['less', browserSync.reload ]);
  gulp.watch(`${paths.glob.js}`, ['lint-js', browserSync.reload ]);

});
