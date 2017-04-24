import gulp from 'gulp';
import util from 'gulp-util';
import proxy from 'proxy-middleware';
import connect from 'gulp-connect';
import browserSync from 'browser-sync';
import modRewrite  from 'connect-modrewrite';
import urlParser from 'url';
import runSequence from 'run-sequence';
import paths from '../paths';

let redirect = (route) => ({
        to : (remoteUrl) => {
            let options = urlParser.parse(remoteUrl);
            options.route = route;
            options.preserveHost = true;
            return proxy(options);
        }
    });

function startBrowserSync(directoryBase, files, browser) {
    browser = browser === undefined ? 'default' : browser;
    files = files === undefined ? 'default' : files;

    browserSync({
        files: files,
        open: true,
        port: 8000,
        notify: true,
        server: {
            baseDir: directoryBase,
            middleware: [
                redirect('/ws').to('http://localhost:8080/ws'),
                redirect('/api').to('http://localhost:8080/api'),
                modRewrite(['!\\.\\w+$ /index.html [L]']) // require for HTML5 mode
            ]
        },
        browser: browser
    });
}

gulp.task('serve', ['watch'], () => {
    startBrowserSync([paths.srcDir, './' ]);
});