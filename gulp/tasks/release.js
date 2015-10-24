import gulp from 'gulp';
import util from 'gulp-util';
import semver from 'semver';
import paths from '../paths';
import conventionalChangelog from 'gulp-conventional-changelog';
import bump from 'gulp-bump';
import git from 'gulp-git';
import runSequence from 'run-sequence';

import pkg from '../../package.json';

let argv = util.env;

gulp.task('bump', (cb) => {
    if (!semver.valid(pkg.version)) {
        util.log(util.colors.red(`Error: Invalid version number - ${pkg.version}`));
        return process.exit(1);
    }

    let hasValidType = !!argv.type ? !!argv.type.match(new RegExp(/major|minor|patch/)) : false;
    if (!hasValidType) {
        util.log(util.colors.red('Error: Required bump \'type\' is missing! Usage: npm release --type=(major|minor|patch)'));
        return process.exit(1);
    }

    pkg.version = semver.inc(pkg.version, argv.type);
    gulp.src([paths.packageJson])
        .pipe(bump({ version: pkg.version }))
        .pipe(gulp.dest('./'))
        .on('end', cb);
});

gulp.task('changelog', (cb) => {
    gulp.src(`${paths.root}/CHANGELOG.md`, { buffer: false })
        .pipe(conventionalChangelog({ preset: 'angular' }))
        .pipe(gulp.dest('./'))
        .on('end', cb);
});

gulp.task('commit-changelog', (cb) => {
    let sources = [ `${paths.root}/CHANGELOG.md`, `${paths.root}/package.json` ];
    gulp.src(sources)
        .pipe(git.add())
        .pipe(git.commit(`chore(release): ${pkg.version}`))
        .on('end', cb);
});

gulp.task('create-version-tag', (cb) => {
    git.tag(`v${pkg.version}`, `release v${pkg.version}`, (err) => {
        if (err) throw err;
        cb();
    });
});

gulp.task('push-to-origin', (cb) => {
    git.push('origin', 'master', { args: '--tags' }, (err) => {
        if (err) throw err;
        cb();
    });
});

gulp.task('release', (cb) => {
    runSequence('bump', 'changelog', 'commit-changelog', 'create-version-tag', 'push-to-origin', cb);
});
