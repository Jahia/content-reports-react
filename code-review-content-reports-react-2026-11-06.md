# Code review — content-reports-react

- Date: 2026-11-06
- Scope: the whole module at `2.1.1-SNAPSHOT` (Java bundle, GraphQL extensions, jContent UI
  extension, build), including the uncommitted report 31 (*Displayed in some languages only*) and
  the site-resolution fix of the same day. Security findings were reproduced on a local Jahia
  8.2.3.2 (Molex demo, port 8085) with a throwaway user who was editor on one site only; the user,
  its role and every file it created were removed afterwards.
- Out of scope: the content of each legacy report's numbers (only report 31 was checked against an
  independent repository count: 30 of 30 rows), the webpack bundle, the CI workflow beyond semgrep.

The content reports themselves are sound: every report runs on the caller's JCR session, so a user
sees only what JCR lets them read (an editor of one site gets nothing from another site), the
GraphQL entry is under `admin`, which refuses guests and users without a role, and the newer
reports (29, 30, 31) return one typed row per item. The problems are concentrated in the users and
groups report, which runs every read, write and delete with a system session behind no permission
check at all, and in the absence of any automated test.

## 1. Security

### 1.1 Any site editor exports the whole user directory and writes or deletes files in any site — 🔴 P0

The `contentReportsUsersGroups` query and mutation carry no permission check, and do everything
with a system session on paths the caller chooses.

`ContentReportsQueryExtension#contentReportsUsersGroups`,
`ContentReportsMutationExtension#contentReportsUsersGroups`,
`GqlContentReportsUsersGroupsMutations#generate` / `#deleteReport` and
`GqlContentReportsUsersGroups#getFiles` have no `@GraphQLRequiresPermission`; the only gate is
`admin`, which a user holding the `editor` role on one site passes. Shown live on 8.2.3.2 with a
user who was only editor on `gba-hk`:

- `generate(csvRootPath:"/sites/gba-hk/files", userPropertiesToExport:["j:email","j:firstName"])`
  answered `true`, and the editor downloaded the CSV: every global and per-site user of the
  platform with email, first name and group memberships. The property list is a block-list
  (`j:password`, `rep:password`), so any other user property can be requested.
- `generate(csvRootPath:"/sites/molex-pro/files")` wrote folders and a file into a site where the
  same user cannot read `/sites/molex-pro/home` (`ReportUsersAndGroupsGenerator#writeCsvData`,
  system session).
- `deleteReport(path:"/sites/molex-pro/files/report-users-and-groups/…csv")` deleted it
  (`isReportFilePath` only checks that the path contains `/report-users-and-groups/`).
- `files(csvRootPath:…)` lists report files of any site through a system session.

The jContent route (`JContent.routes.jsx`, `contentReportReact-usersGroups-*`) has no
`requiredPermission` either, so the screen shows to every editor; hiding it would not fix the API.

**Fix:** put `@GraphQLRequiresPermission("adminUsers")` (server-level user administration) on the
three fields of both extension classes, or check it once in the constructors of
`GqlContentReportsUsersGroups` and `GqlContentReportsUsersGroupsMutations`; keep the system session
for reading users only after that check. Fix the output location instead of taking it from the
caller (the job's configured root), refuse `deleteReport` outside it, and replace the property
block-list with an allow-list from configuration. Add `requiredPermission: 'adminUsers'` to the two
routes. Ship a test that signs in as a site editor and expects each field refused, and one as an
administrator that expects success (§7.1).

**Effort:** S

**Status (2026-11-06):** fixed, unreleased. Both extension fields carry
`@GraphQLRequiresPermission("adminUsers")`; `generate` refuses a target that is not a `jnt:folder`
the caller can add children to (caller's session); `files` and `deleteReport` use the caller's
session, and `deleteReport` removes `jnt:file` nodes only; generation and deletion log the user;
both jContent routes carry `requiredPermission: 'adminUsers'`. Verified on 8.2.3.2 with a one-site
editor: all six fields answer `Permission denied`, the menu entry is gone and its URL answers "App
not found", content reports still work. As root: generate, list, download and delete work; a page
or missing folder as target and a folder as delete path are refused. Still open: the property
block-list (acceptable now that only user administrators reach it) and the automated tests (§7.1).

### 1.2 Report request values are concatenated into JCR-SQL2 — 🟡 P2

Several reports build their query from request parameters without escaping.

`ReportByDateAndAuthor` writes `dateBegin`, `dateEnd`, `searchUsername` and `sortOrder` into the
statement (`" AND item.["+usernameField+"] = '"+searchUsername+"'"`), `ReportBeforeDate` writes
`strDate`, `ReportByAuthorDetailed` writes `username`. `ContentReportFactory#cleanPath` removes `'`
from paths only. The queries run on the caller's session, so a crafted value cannot read more than
the caller already can, but it can leave the site scope of the report or make it fail.

**Fix:** validate dates with `LocalDate.parse`, escape strings with
`JCRContentUtils.sqlEncode`, accept `ASC`/`DESC` only.

**Effort:** XS

### 1.3 Report cells typed `html` are rendered with `dangerouslySetInnerHTML` — 🟢 P3

`ReportResultsTable` renders `type: 'html'` columns as raw HTML. Today they carry node type names
joined with `<br/>` and visibility conditions built from constrained values (dates, hours, a day
choice list), so no editor text reaches them (semgrep `jahia-react-dangerouslysetinnerhtml-unsanitized`,
triaged).

**Fix:** return arrays from the server and render them as text lines, then drop the `html` type.

**Effort:** XS

### 1.4 Counts skip ACL checks — 🟢 P3

`ReportOverview`, `ReportComponentUsage` and `ReportContentMarkedForDeletion` count with
`rep:count(item,skipChecks=1)`, which counts nodes the caller cannot read. Only numbers leak, on a
site the caller can open. The 2.1.0 changelog says component counts "run on the caller's session,
so JCR ACLs apply", which is not true for the count.

**Fix:** accept and document (the counts are the point and are cheap), and correct the changelog
sentence; or count without `skipChecks` where the number must match what the user sees.

**Effort:** XS

## 3. Bugs and rough edges

### 3.1 Reports ran on the server's site instead of the site open in jContent — 🟠 P1 (fixed in this change)

`AdminPanel` read `window.contextJsParameters.siteKey`, which on an instance where sites share the
server name `localhost` (Jahia Cloud, local Docker) is `systemsite`: every report title, default
search path and the path picker showed `systemsite` while jContent was on another site. Fixed in
the uncommitted change: the panel reads `site`, `language` and `uilang` from jContent's store
(`window.jahia.reduxStore`) and remounts per site (`key={siteKey}`). Verified in jContent on
`gba-hk`.

**Fix:** done; release it.

**Effort:** XS

### 3.2 Results are silently cut — 🟡 P2

`ReportByUnstranslated#execute` reads at most 1000 nodes ("temporary limit until a real server
side pagination is implemented") and the panel asks every report for 10000 rows
(`AdminPanel.jsx`, `limit: selectedReport.defaultLimit ?? 10000`). A larger site gets a partial
table and no warning.

**Fix:** return `recordsTotal` from a count and show a banner when it exceeds the rows returned; or
paginate on the server.

**Effort:** S

### 3.3 A site the caller cannot read answers "Internal Server Error" — 🟢 P3

`GqlContentReports` calls `session.getNode("/sites/" + siteKey)`; a site the caller has no access
to throws `PathNotFoundException`, logged as an ERROR and returned as a generic server error.

**Fix:** catch it and raise a `GqlAccessDeniedException` ("site not found or not accessible").

**Effort:** XS

### 3.4 Debug logging left in the UI — 🟢 P3

`AdminPanel.jsx`, `ReportResultsTable.jsx` and `index.js` call `console.log` (for example
`[AdminPanel] openPathPicker - fieldName: …`).

**Fix:** remove them or switch to `console.debug`.

**Effort:** XS

### 3.5 Three reports read a resource bundle the module does not ship — 🟠 P1

Reports 2, 6 and 24 call `Messages.get("resources.contentReportReact", ...)` (month names, status
names, published / not published), and the module has no `src/main/resources/resources/`.

`ReportByAllDate` (report 2, *By date*) always calls it and answers `Internal Server Error` with
`MissingResourceException: Can't find bundle for base name resources.contentReportReact`
(reproduced on 8.2.3.2). `ReportByStatus` (6) and `ReportContentMarkedForDeletion` (24) fail the same
way as soon as they have a row to label; on a site without such content they return an empty table,
so the defect hides.

**Fix:** return the keys (`month.<n>`, `status.<name>`, `published`) and translate them in the UI
from the existing locale JSON files, which already ship six languages; or ship
`resources/contentReportReact*.properties` (ASCII, `\uXXXX` escapes) with the keys the three
reports use.

**Effort:** S

**Status (2026-11-06):** fixed, unreleased. Report 2 takes month names from
`Month#getDisplayName` in the report language; reports 6 and 24 read their labels through
`BaseReport#label`, which loads `resources/contentReportReact*.properties` (en base, fr, de, es, it, pt)
with the module's class loader and no fallback to the server locale. `ReportLabelsTest` checks every
key in every file, ASCII content, and the fallback (its mutant, the default fallback, fails it). Not
yet run on a Jahia instance (the local one was stopped).

## 5. Build, Maven and OSGi packaging

### 5.1 The 2.1.0 release jar was built on a laptop — 🟡 P2

The jar attached to the GitHub release `content-reports-react-2.1.0` has
`Jahia-Source-Folders: /Users/stephane/Runtimes/0.Modules/contentReportRe…` and no
`Jahia-Signature`, so it was built locally, not by CI.

**Fix:** build the release jar in CI (the release workflow of `jahia-release-and-ci.md`) from the
next release on.

**Effort:** S

### 5.2 The front-end build runs Node 20 — 🟢 P3

`frontend-maven-plugin` installs `v20.14.0` with Yarn 1; the harness pins Node 22 for UI
extensions. Builds pass today.

**Fix:** move to Node 22 when the next dependency update touches the build.

**Effort:** XS

`Import-Package` of the snapshot is identical to 2.1.0 (47 entries), built with Maven 3.9 since no
3.8 was installed; worth rebuilding with 3.8 before the release.

## 7. Tests

### 7.1 No automated test — 🟡 P2

There is no `src/test` and no Cypress suite: neither the reports nor the GraphQL permissions are
checked after a change. §1.1 would have been caught by one refusal test.

**Fix:** a Cypress suite on the Jahia CI image: for `contentReports.rawReport`, one success case per
report family and a cross-site refusal; for `contentReportsUsersGroups`, refusal for a site editor
on each field (each test passing every other check, so only the permission refuses it) and success
for an administrator. Unit tests for `ReportUsersAndGroupsGenerator#normalizeRootPath` /
`#isReportFilePath` and `ContentReportFactory#cleanPath`.

**Effort:** M

## 8. Prioritised summary

| Priority | Item | Effort |
| --- | --- | --- |
| 🔴 P0 | Users and groups report: no permission, system-session export, write and delete in any site (§1.1) — fixed, unreleased | S |
| 🟠 P1 | Reports ran on `systemsite` instead of the site open in jContent — fixed, unreleased (§3.1) | XS |
| 🟠 P1 | Reports 2, 6 and 24 fail on a missing resource bundle (§3.5) — fixed, unreleased | S |
| 🟡 P2 | Request values concatenated into JCR-SQL2 (§1.2) | XS |
| 🟡 P2 | Results silently cut at 1000 / 10000 rows (§3.2) | S |
| 🟡 P2 | Release jar built locally, unsigned (§5.1) | S |
| 🟡 P2 | No automated test (§7.1) | M |
| 🟢 P3 | `html` cells rendered raw (§1.3) | XS |
| 🟢 P3 | ACL-skipping counts, changelog claim (§1.4) | XS |
| 🟢 P3 | Inaccessible site answers a server error (§3.3) | XS |
| 🟢 P3 | `console.log` left in the UI (§3.4) | XS |
| 🟢 P3 | Front-end build on Node 20 (§5.2) | XS |

Not verified: whether the nightly job's CSV under `/sites/systemsite/files` is readable by
non-administrators (no job had run on the test instance), and which permission makes `admin`
reachable for a site editor (it is the provider's gate, not this module's).

## 9. Closing note

The biggest risk is §1.1: one mutation lets any editor of any site download the platform's user
directory, and it is a small, well-located fix. The biggest strength is that every content report
runs on the caller's own session, so the reports cannot show a user content JCR would hide from
them.
