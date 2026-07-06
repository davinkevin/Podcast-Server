const {
  fromPropertiesFile,
  inGradleUserHome,
} = require('@gradle-tech/develocity-agent/api/config');

/**
 * Standard (Gradle-independent) Develocity npm agent configuration.
 *
 * The agent is preloaded on every `npm run <script>` via `.npmrc`
 * (`node-options=-r @gradle-tech/develocity-agent/preload`), so Build Scans
 * are produced whether npm is run by hand, in CI, or through Gradle.
 *
 * The access key is reused from the Develocity Gradle plugin location
 * (`~/.gradle/develocity/keys.properties`) so no extra secret is needed.
 */
module.exports = {
  server: {
    url: process.env.DEVELOCITY_URL || 'https://develocity.davinkevin.fr',
    accessKey: fromPropertiesFile(inGradleUserHome()),
  },
};
