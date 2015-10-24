
import path from 'path';

/* All constant and location of the application */

const appName = 'app';
const srcDirName = 'public';
const releaseDirName = 'dist';
const root = path.dirname(__dirname);

export default {
  root : root,
  systemConfigJs : `${root}/system.config.js`,
  packageJson : `${root}/package.json`,
  changeLog : `${root}/CHANGELOG.md`,
  srcDir: `${root}/${srcDirName}`,
  releaseDir: `${root}/${releaseDirName}`,
  releaseDirName: releaseDirName,
  app: {
    entryPoint : `${srcDirName}/${appName}`,
    name: appName
  },
  glob: {
    less : `${root}/${srcDirName}/**/*.less`,
    js : `${root}/${srcDirName}/**/!(*.spec).js`
  }
}
