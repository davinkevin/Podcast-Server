import gulp from 'gulp';
import util from 'gulp-util';
import browserSync from 'browser-sync';
import proxy from 'proxy-middleware';
import connect from 'gulp-connect';
import urlParser from 'url';
import runSequence from 'run-sequence';
import paths from '../paths';

let redirect = (route) => {
  return {
    to : function(remoteUrl) {
      let options = urlParser.parse(remoteUrl);
      options.route = route;
      options.preserveHost = true;
      return proxy(options);
    }
  }
};

function startServer(directoryBase) {

  connect.server({
    root: directoryBase,
    port: 8000,
    livereload: true,
    fallback: `${paths.srcDir}/index.html`,
    middleware: function() {
      return [
        redirect('/api').to('http://localhost:8080/api'),
        redirect('/ws').to('http://localhost:8080/ws')
      ];
    }
  });

}

gulp.task('serve', ['less'], () => {

  startServer([paths.srcDir, paths.root ]);

  gulp.watch(`${paths.glob.less}`, ['less', connect.reload ]);
  gulp.watch(`${paths.glob.js}`, ['lint-js', connect.reload ]);

});
