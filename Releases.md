## 2010-11-18: v1.09 ([r133](https://code.google.com/p/devoxx2010/source/detail?r=133)) ##
  * Find the sessions coming up next during breaks easy by clicking on 'No sessions nearby.' on the main screen.
  * Pinned header list view for speakers.
  * Correct height calculation for focus to current/next session.

## 2010-11-14: v1.08 ([r125](https://code.google.com/p/devoxx2010/source/detail?r=125)) ##
  * visualize sessions with a pinned header listview
  * update of initial local cache
  * fix issue to report correct parallel starred session count
  * validation of activation code for MySchedule
  * focus list to current/next session during conference
  * browse sessions by room

## 2010-11-07: v1.07 ([r109](https://code.google.com/p/devoxx2010/source/detail?r=109)) ##
  * support for small screens like HTC Wildfire
  * add AlphabetIndexer to tags overview
  * added tab to browse sessions by type in the sessions overview
  * sessions overview now also shows sessions count for track, type, tag
  * fix issue where tags/search/sessions was detected as search uri
  * extra indices in db for faster UI

## 2010-11-04: v1.06 ([r92](https://code.google.com/p/devoxx2010/source/detail?r=92)) ##
  * update of static lab sessions json responses
  * fix issue with device rotation during email/publish MySchedule
  * support for CFP presentation tags
  * update of initial local cache

## 2010-10-28: v1.05 ([r75](https://code.google.com/p/devoxx2010/source/detail?r=75)) ##
  * fix wrong starred session count over different blocks
  * update of static lab sessions json responses
  * let a change in the labs sessions also trigger a remote sync
  * speaker image now with border and shadow

## 2010-10-27: v1.04 ([r65](https://code.google.com/p/devoxx2010/source/detail?r=65)) ##
  * support of labs sessions
  * update of initial local cache
  * note edit now has sentences capitalization

## 2010-10-21: v1.03 ([r50](https://code.google.com/p/devoxx2010/source/detail?r=50)) ##
  * update of initial local cache
  * notes activity supports better overview/edit/delete of notes
  * update of search suggest table
  * fix of caching problem on appengine for md5-key calculation
  * fix of sync-bug where speakers could be linked with non-existing sessions
  * added alphabet indexer to speakers list overview
  * recreation of full text search table, fixing double entries
  * title added to MySchedule alert dialogs

## 2010-10-07: v1.02 ([r6](https://code.google.com/p/devoxx2010/source/detail?r=6)) ##
  * update of initial local cache
  * fix of fulltext search table contents for sessions where note content was null
  * input type restrictions on the input fields for the MySchedule registration

## 2010-10-06: v1.01 ##
  * update of initial local cache
  * email & publish to the Devoxx wiki of your starred sessions
  * setting to enable/disable highlighting of sessions in the starred overview that occur at the same time
  * session details now also shows note information when available for a session
  * fix of sync bug that could show sessions without room and block info

## 2010-09-30: v1.00 ##
  * Initial version