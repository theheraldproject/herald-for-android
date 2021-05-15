# How GitHub Actions is used

We aim to have the following steps:-

- Unit tests check on PR being issued to Develop or master/main branches
  - unit_tests.yml
  - Prevents regressions
  - Also useful incase a develop forgets to run locally, or thinks "It's just a doc change, it'll be fine"

Currently not done:-

- No automation to auto release when a mileston is complete - requires manual PR from develop to master, and manual release notes with 'v' tag
- No code coverage for tests
- No multi-android version tests
- We don't automatically retest existing code when a new android version is released (We should probably do this for every single beta release and full release)


Later:-


- Build release on seeins a 'v' tag (E.g. v2.1)
  - see https://www.raywenderlich.com/19407406-continuous-delivery-for-android-using-github-actions
  - issue_release.yml
  - Build and sign final APK for statically linked Herald library
  - Build and push maven release