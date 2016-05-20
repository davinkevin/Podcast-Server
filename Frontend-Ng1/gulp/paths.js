
import path from 'path';

/* All constant and location of the application */

const appName = 'app';
const srcDirName = 'www';
const releaseDirName = 'target/dist';
const root = path.dirname(__dirname);
const fontsExtension = '{eot,woff2,woff,ttf,svg}';

export default {
  root : root,
  systemConfigJs : `${root}/system.config.js`,
  packageJson : `${root}/package.json`,
  pomXml : `${root}/../Backend//pom.xml`,
  changeLog : `${root}/CHANGELOG.md`,
  srcDir: `${root}/${srcDirName}`,
  releaseDir: `${root}/${releaseDirName}`,
  release : {
    root : `${root}/${releaseDirName}`,
    fonts : `${root}/${releaseDirName}/fonts`
  },
  releaseDirName: releaseDirName,
  jspm : {
    fonts : `${root}/${srcDirName}/jspm_packages/**/*.${fontsExtension}`
  },
  app: {
    app : `${root}/${srcDirName}/${appName}`,
    entryPoint : `${appName}/${appName}`,
    name: appName,
    fonts : `${root}/${srcDirName}/fonts`
  },
  glob: {
    less : `${root}/${srcDirName}/${appName}/**/*.less`,
    js : `${root}/${srcDirName}/${appName}/**/!(*.spec).js`,
    fonts : `${root}/${srcDirName}/fonts/**/*.${fontsExtension}`,
    projectFonts : `${root}/${srcDirName}/**/*.${fontsExtension}`,
    html : `${root}/${srcDirName}/**/*.html`
  }
}