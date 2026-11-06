# Content Report React Module

A comprehensive content reporting tool for Jahia DX, built with React and the Moonstone UI library. This module provides powerful insights into your Jahia site's content through various customizable reports.

## Overview

The Content Report React module offers a modern, user-friendly interface for generating and viewing detailed reports about your Jahia site content. It features a site overview dashboard and multiple specialized reports covering content, languages, visibility, metadata, and system information.

## Features

### 📊 Site Overview Dashboard
- **Content Metrics**: Pages, deployed modules, registered users, content nodes (`jnt:content`), editorial contents (`jmix:editorialContent`)
- **Workflow Tracking**: Pending workflow tasks count
- **Asset Management**: Files (`jnt:file`) and Images (`jmix:image`) counts
- **Language Support**: Available site languages count with visual display
- **Content Activity (Last 30 Days)**:
  - New content created
  - Modified content items
  - Published content items
  - Published vs Unpublished nodes comparison
  - Average time from creation to publication
  - Top contributors ranked by content count (with medal badges 🥇🥈🥉)
  - **Detailed activity log**: a collapsible table listing the content behind those
    counters — every `jmix:editorialContent` node created, modified or published in the
    window, with its creation, last-modification and last-publication dates and the user
    responsible for each. Tags mark which of the three events actually fell inside the
    window. Sortable on any column, filterable per event type, paginated, exportable to
    CSV or JSON, and each path links straight into jContent. Capped at the 500 most
    recently touched items, with the full total reported alongside.

    Rows are read with the caller's own JCR session, so the table only ever lists content
    the current user is allowed to see.

### 📝 Content Reports
- **By Author and Date**: Filter pages or all content by combining author and date range criteria; supports creation vs. modification date toggle
- **By Author**: List content created or last modified by a specific author
- **By All Dates**: Break down content activity per month and type, with creation or modification date scope
- **Before Date**: List content created or modified before a given date
- **By Type**: Analyze content distribution grouped by node type
- **By Type (Detailed)**: Same as By Type with additional per-node detail rows
- **By Status**: View content filtered by publication status
- **Work in Progress**: Content marked as WIP — still being edited, not ready for publication
- **Workflow Instances**: All content with a pending workflow task (awaiting review, approval, or publication)
- **Marked for Deletion**: Content flagged for removal, including child node counts and publication status

### 🔗 References Reports
- **References**: Content references between two paths — only shows content actually used on a page
- **Unused Assets**: Files under a chosen path that are not referenced anywhere in the site, with MIME type icons and direct media folder links

### 🌍 Language Reports
- **Languages**: Content breakdown across all site languages
- **Language Detail**: Per-language detail for a specific target language
- **Pages Without Title**: Pages missing a title in one or more languages
- **Untranslated Content**: Content existing in other languages but missing a translation for the selected target language

### 👁️ Visibility Reports
- **Live Visibility Conditions**: Content with active conditional visibility rules, showing conditions, match status, and current visibility state
- **Expired Content**: Content whose visibility end date has already passed (no longer live)
- **Scheduled Content**: Content with a future visibility start date not yet reached
- **Displayed in some languages only**: Pages and content switched off in some of the site's languages (Content Editor, Visibility > Languages), with the languages each is displayed in and hidden in. Page Builder shows them in every language; preview and live leave them out

### 🔍 Metadata Reports
- **Pages Without Keywords**: Pages missing SEO keyword metadata
- **Pages Without Description**: Pages missing descriptions in one or more languages

### 👥 Users & Groups Report
- **Report Users & Groups**: Generate a CSV report of all Jahia users across all sites, including group memberships; reports are stored in JCR and downloadable directly from the page
- Requires the server permission `adminUsers` (server administrators). The nightly job writes to `jahia.contentReports.usersGroups.job.csvRootPath` (default `/sites/systemsite/files`): keep that folder readable by administrators only

### ⚙️ System Reports
- **Locked Content**: All content currently locked by users, showing creator, lock holder, and location
- **Custom Cache Content**: Content with custom cache expiration settings, showing expiration values to help optimize cache performance
- **ACL Inheritance Break**: Nodes where ACL inheritance has been broken (custom permissions overriding parent), useful for security auditing
- **Modules deployed on site**: Every module deployed on the site with its version, type, bundle state and how it got there (template set, installed on the site, or transitive dependency), plus the number of component types each one declares
- **Component usage**: For each deployed module, every component type it declares and how many nodes of exactly that type exist under the site — zero-usage components included, so unused components are easy to find (sort the usage column ascending)

## Requirements

- **Jahia DX**: 8.2.0.0 or higher
- **Java**: 11 or higher
- **Node.js**: 20.14.0 (via frontend-maven-plugin)
- **Yarn**: 1.22.19

## Installation

### From Source

1. Clone the repository:
```bash
git clone https://github.com/jahia/contentReportReact.git
cd contentReportReact
```

2. Build the module:
```bash
mvn clean install
```

3. Deploy the generated JAR to your Jahia server:
```bash
cp target/content-reports-react-*.jar $JAHIA_HOME/modules/
```

### From Binary

1. Download the latest release JAR
2. Copy to your Jahia modules directory
3. Restart Jahia or deploy via Module Management

## Usage

### Accessing the Module

1. Log in to Jahia as an administrator
2. Navigate to **Additional** From your Site jContent
3. Select **Content Report** from the settings menu

### Running Reports

1. **Select a Report**: Choose from the categorized menu on the left
2. **Configure Parameters**: 
   - Set the content path (use Browse button for picker)
   - Configure date ranges, filters, and other options
   - Enable/disable optional filters via checkboxes
3. **Execute**: Click "Run Report" button
4. **View Results**: Results display in a sortable, paginated table

### Report Categories

- **Overview**: Site-wide statistics and metrics
- **Content**: Content analysis and filtering reports
- **Languages**: Translation and localization reports
- **Visibility**: Publication status and access control reports
- **Metadata**: SEO and metadata completeness reports
- **System**: Technical and administrative reports

## Report Columns

Reports support various column types:
- **Text**: Standard text display
- **Number**: Numeric values, sorted numerically (a `0` displays as `0`, not as an empty cell)
- **Date**: Formatted date/time values
- **Boolean**: Checkmark/cross indicators
- **Link**: Clickable content links
- **HTML**: Rich formatted content
- **i18n**: Internationalized property values

## Configuration

### Custom Reports

Reports are configured in `src/javascript/AdminPanel/AdminPanel.constants.js`:

```javascript
{
    id: 'myReport',
    labelKey: 'menu.myReport',
    descriptionKey: 'descriptions.myReport',
    type: 'legacy',
    category: 'content',
    fields: [
        {name: 'pathTxt', type: 'path', labelKey: 'fields.path', required: true},
        // ... more fields
    ],
    columns: [
        {key: 'title', labelKey: 'columns.title', sortable: true},
        // ... more columns
    ]
}
```

### Translations

The module supports 6 languages with complete translations:
- `src/main/resources/javascript/locales/en.json` (English)
- `src/main/resources/javascript/locales/fr.json` (French)
- `src/main/resources/javascript/locales/de.json` (German)
- `src/main/resources/javascript/locales/es.json` (Spanish)
- `src/main/resources/javascript/locales/it.json` (Italian)
- `src/main/resources/javascript/locales/pt.json` (Portuguese)

**Content Activity Translation Keys** (added for all languages):
```json
{
  "result": {
    "contentActivity": "Content Activity (Last 30 Days)",
    "newContentLast30Days": "New Content Created",
    "modifiedContentLast30Days": "Modified Content Items",
    "publishedContentLast30Days": "Published Content Items",
    "unpublishedNodes": "Unpublished Nodes",
    "publishedNodes": "Published Nodes",
    "averageTimeToPublish": "Average Time to Publish",
    "days": "days",
    "topContributors": "Top Contributors",
    "items": "items",
    "noContributorsData": "No contributors data available for the last 30 days"
  }
}
```

### Backend Reports

Create custom report beans by extending `BaseReport` or `QueryReport`:

```java
@Component
public class MyCustomReport extends QueryReport {
    @Override
    public String getId() {
        return "myReport";
    }
    
    @Override
    protected String buildQuery(Map<String, String> parameters) {
        // Build JCR SQL-2 query
        return "SELECT * FROM [jnt:page] WHERE ISDESCENDANTNODE('" + path + "')";
    }
}
```

## Development

### Project Structure

```
content-reports-react/
├── src/
│   ├── javascript/          # React frontend
│   │   ├── AdminPanel/      # Main UI components
│   │   │   ├── reports/     # Report-specific components
│   │   │   └── AdminPanel.constants.js
│   │   ├── graphql/         # GraphQL queries
│   │   └── styles/          # SCSS styles
│   └── main/
│       ├── java/            # Java backend
│       │   └── org/jahia/modules/contentreports/
│       │       ├── bean/    # Report implementations
│       │       ├── graphql/ # GraphQL resolvers
│       │       └── service/ # Business logic
│       └── resources/
│           ├── definitions/ # GraphQL schema
│           └── javascript/  # Built assets
│               ├── apps/    # Webpack output
│               └── locales/ # i18n files
├── package.json
├── pom.xml
└── webpack.config.js
```

### Building for Development

```bash
# Install dependencies
yarn install

# Development mode with watch
yarn dev

# Production build
yarn build:production

# Lint code
yarn lint
yarn lint:fix

# Clean build artifacts
yarn clean
```

### Adding a New Report

1. **Define Report Configuration** (`AdminPanel.constants.js`):
   ```javascript
   {
       id: 'newReport',
       labelKey: 'menu.newReport',
       type: 'legacy',
       category: 'content',
       fields: [...],
       columns: [...]
   }
   ```

2. **Add Translations** (`en.json`, `fr.json`):
   ```json
   {
       "menu": {
           "newReport": "New Report"
       },
       "descriptions": {
           "newReport": "Description of the new report"
       }
   }
   ```

3. **Create Backend Report** (Java):
   ```java
   @Component
   public class ReportNewReport extends QueryReport {
       @Override
       public String getId() {
           return "newReport";
       }
       
       @Override
       protected String buildQuery(Map<String, String> parameters) {
           // Implement query logic
       }
   }
   ```

4. **Build and Test**:
   ```bash
   mvn clean install -DskipTests
   ```

## GraphQL API

The module exposes GraphQL queries for accessing reports:

```graphql
query {
  admin {
    contentReports {
      overview(siteKey: "/sites/mysite", language: "en") {
        siteName
        nbPages
        nbContents
        nbFiles
        nbImages
        languages
        # Content Activity (Last 30 Days)
        newContentLast30Days
        modifiedContentLast30Days
        publishedContentLast30Days
        unpublishedNodes
        publishedNodes
        averageTimeToPublish
        topContributors {
          username
          contentCount
        }
        # Detailed activity log — capped at 500 rows, most recent first
        recentActivityTotal
        recentActivity {
          name
          path
          type
          created
          createdBy
          lastModified
          lastModifiedBy
          lastPublished
          lastPublishedBy
          isNew
          isModified
          isPublished
        }
      }
      rawReport(
        siteKey: "mysite"
        language: "en"
        reportId: "20"
        offset: 0
        limit: 100
        parameterName1: "pathTxt"
        parameterValue1: "/sites/mysite"
      )
    }
  }
}
```

## Technologies

### Frontend
- **React** 18.2.0: UI framework
- **Moonstone** 2.16.2: Jahia's design system
- **i18next**: Internationalization
- **Axios**: HTTP client
- **Webpack** 5: Module bundler

### Backend
- **Java** 11: Backend language
- **GraphQL**: API layer (graphql-java-annotations)
- **JCR**: Content repository queries
- **Spring**: Dependency injection
- **OSGi**: Module framework

## Performance Considerations

- Reports use `rep:count(item, skipChecks=1)` for efficient counting
- Client-side pagination for large datasets (default limit: 10,000)
- Lazy loading of report results
- Optimized JCR queries with proper indexing

## Troubleshooting

### Build Issues

**Problem**: Frontend build fails
```bash
yarn clean:all
yarn install
mvn clean install
```

**Problem**: Java compilation errors
- Ensure Java 11+ is installed
- Check Jahia parent POM version compatibility

### Runtime Issues

**Problem**: Reports return no data
- Verify user has read permissions on content
- Check JCR query syntax in logs
- Ensure site key and language are correct

**Problem**: Path picker not working
- Verify Content Editor API is available
- Check browser console for JavaScript errors

### Debugging

Enable debug logging in Jahia:
```
log4j.logger.org.jahia.modules.contentreports=DEBUG
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit changes: `git commit -am 'Add new feature'`
4. Push to branch: `git push origin feature/my-feature`
5. Submit a pull request

### Code Standards

- Follow ESLint rules for JavaScript
- Use Java code formatting standards
- Add translations for all new strings
- Document complex logic with comments
- Write meaningful commit messages

## License

MIT License

Copyright (c) 2002 - 2022 Jahia Solutions Group. All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Support

For issues, questions, or contributions:
- GitHub Issues: [https://github.com/jahia/contentReportReact/issues](https://github.com/jahia/contentReportReact/issues)
- Jahia Community: [https://community.jahia.com](https://community.jahia.com)

## Changelog

Release notes live in [CHANGELOG.md](CHANGELOG.md), one section per version, following
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Each release is also published on the
repository's [Releases](https://github.com/Jahia/contentReportReact/releases) page with the module
jar attached.

## Roadmap

- [x] Export reports to CSV and JSON (every report table, plus the Site overview activity log)
- [ ] Scheduled report generation
- [ ] Email notifications for report results
- [ ] Custom report builder UI
- [ ] Advanced filtering and search
- [ ] Report templates and presets
- [ ] Historical data tracking
- [ ] Additional language support

---

**Made with ❤️ for the Jahia Community**
