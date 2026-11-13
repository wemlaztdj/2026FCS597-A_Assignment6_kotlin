# Connecting the pages to the shared database

The current checkout has no database implementation. Balances and flowers are
demo content. Use the team's existing database schema and data-access functions;
do not create a second database or separate balance variables for each screen.

## Navigation and page ownership

MainActivity inflates each XML into page_container. These pages are not separate
Activities. Keep the bottom navigation in activity_main.xml and feature logic in
separate Kotlin files to reduce merge conflicts.

| Page key | Layout | Integration point |
| --- | --- | --- |
| home | page_home.xml | HomePage.bind runs every time Home opens |
| withdraw | page_take_out.xml | TakeOutFundsPage.renderAvailableFunds |
| garden | page_garden.xml | garden_container holds the sample flower rows |
| details | page_details.xml | fund_details_text |
| water | page_water_delivery.xml | water_delivery_date_text |

Home's balance TextViews have IDs overall_funds_text and leftover_funds_text.
Call HomePage.renderBalances with formatted amounts loaded from the database.
home_garden_container contains Home's flower preview; garden_container contains
the full garden. Replace their sample child views when rendering saved seeds.
An empty database should show an empty garden, rather than the sample flowers.

## Feature integration

- Add Seed: save through the existing database layer, then return to garden/home.
- Add Funds: update the shared balance through that layer, then return home.
- Take Out Funds: add the form to page_take_out.xml and its logic to
  TakeOutFundsPage.kt. Validate the amount and available balance before writing.
- Agree with the database owner on what overall and leftover mean, whether funds
  belong to a seed or the whole garden, and how money is stored. Follow the
  existing schema; avoid floating-point arithmetic for money.
- Keep database reads/writes off the main thread. Apply view updates on the main
  thread. If observing data, use lifecycle-aware collection and stop observing
  when that page is removed. Do not retain removed page views in callbacks.
- Read fresh data each time a page opens, or observe the shared database. Merely
  reinflating a layout still shows demo values until the read is connected.

Add Seed and Add Funds do not yet have screens or navigation entry points in this
checkout. Coordinate their page keys and links before editing MainActivity. The
HomePage navigation callback is the existing route into MainActivity.showPage.

## Before merging

Sync with the database branch and resolve shared files together. Run the app and
check that adding a seed changes the garden, adding funds changes both relevant
balances, and a withdrawal persists after reopening the app. Confirm rejected
withdrawals do not change the balance. Test empty data and rotation as well.

Commit source, layouts, drawable assets, and required Gradle files. The root
.gitignore excludes artwork-originals, tree-import, local.properties, IDE state,
and build output. Ignore rules do not untrack files already committed. Preserve
the Gradle wrapper JAR even if an older ignore rule excludes JARs globally.
