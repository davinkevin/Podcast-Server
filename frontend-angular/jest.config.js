module.exports = {
  "globals": {
    "ts-jest": {
      "tsConfigFile": "src/tsconfig.spec.json"
    },
    "__TRANSFORM_HTML__": true
  },
  "transform": {
    "^.+\\.(ts|js|html)$": "<rootDir>/node_modules/jest-preset-angular/preprocessor.js",
    "^.+\\.js$": "babel-jest"
  },
  "testMatch": [
    "**/__tests__/**/*.+(ts|js)?(x)",
    "**/+(*.)+(spec|test).+(ts|js)?(x)"
  ],
  "moduleFileExtensions": [
    "ts",
    "js",
    "html",
    "json"
  ],
  "moduleNameMapper": {
    "#app/(.*)": "<rootDir>/src/app/$1"
  },
  "transformIgnorePatterns": [
    "node_modules/(?!@ngrx|ng2-truncate)"
  ],
  "snapshotSerializers": [
    "<rootDir>/node_modules/jest-preset-angular/AngularSnapshotSerializer.js",
    "<rootDir>/node_modules/jest-preset-angular/HTMLCommentSerializer.js"
  ],
  "preset": "jest-preset-angular",
  "setupTestFrameworkScriptFile": "<rootDir>/src/test.ts",
  "coveragePathIgnorePatterns": [
    "/node_modules/",
    "<rootDir>/src/test.ts"
  ]
};
