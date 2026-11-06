import {registry} from '@jahia/ui-extender';
import React, {Suspense} from 'react';
import AdminPanel from './AdminPanel';
import {DEFAULT_ROUTE} from './AdminPanel.constants';

const MODULE_KEY = 'content-reports-react';

// Helper to create a render function for a specific report
const createReportRender = reportId => {
    return v => {
        // Create a mock match object that simulates routing to the specific report
        const mockMatch = {
            ...v.match,
            params: {
                ...v.match?.params,
                reportId
            }
        };

        // Remount per site: the panel keeps the previous site's fields and results otherwise
        const siteKey = window.jahia?.reduxStore?.getState?.()?.site;

        return (
            <Suspense fallback="loading...">
                <AdminPanel key={siteKey} match={mockMatch} initialReportId={reportId}/>
            </Suspense>
        );
    };
};

// Registers a selectable report entry under a category: route key, menu label and target all
// follow from the category and the report key
const addReportRoute = (category, reportKey, reportId) => {
    registry.add('adminRoute', `contentReportReact-${category}-${reportKey}`, {
        targets: [`jcontent-contentReportReact-${category}`],
        isSelectable: true,
        label: `contentReportReact:menu.${reportKey}`,
        requireModuleInstalledOnSite: MODULE_KEY,
        render: createReportRender(reportId)
    });
};

export const registerJContentRoutes = () => {
    // Load i18n namespace
    window.jahia.i18n.loadNamespaces('contentReportReact');

    // Main content reports entry in jContent left nav (not selectable, acts as category)
    registry.add('adminRoute', 'jcontent-contentReports', {
        targets: ['jcontent:1'],
        isSelectable: true,
        icon: window.jahia.moonstone.toIconComponent('Pie'),
        label: 'contentReportReact:label',
        path: `${DEFAULT_ROUTE}*`, // Catch everything and let the app handle routing logic
        defaultPath: DEFAULT_ROUTE,
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('overview')

    });

    // === CONTENT REPORTS ===

    // By Author and Date
    registry.add('adminRoute', 'contentReportReact-byAuthorAndDate', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: true,
        label: 'contentReportReact:menu.byAuthorAndDate',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('20')
    });

    // WIP Content
    registry.add('adminRoute', 'contentReportReact-wipContent', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: true,
        label: 'contentReportReact:menu.wipContent',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('22')
    });

    // Content Waiting Publication
    registry.add('adminRoute', 'contentReportReact-contentWaitingPublication', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: true,
        label: 'contentReportReact:menu.contentWaitingPublication',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('16')
    });

    // Marked For Deletion
    registry.add('adminRoute', 'contentReportReact-markedForDeletion', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: true,
        label: 'contentReportReact:menu.markedForDeletion',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('24')
    });

    // References section label (not selectable)
    registry.add('adminRoute', 'contentReportReact-reference-entry', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.references',
        requireModuleInstalledOnSite: MODULE_KEY
    });

    // References
    registry.add('adminRoute', 'contentReportReact-references', {
        targets: ['jcontent-contentReportReact-reference-entry'],
        isSelectable: true,
        label: 'contentReportReact:menu.references',
        requireModuleInstalledOnSite: MODULE_KEY,
        render: createReportRender('23')
    });
    // Unused assets
    registry.add('adminRoute', 'contentReportReact-unreferencedAssets', {
        targets: ['jcontent-contentReportReact-reference-entry'],
        isSelectable: true,
        label: 'contentReportReact:menu.unusedAssets',
        requireModuleInstalledOnSite: MODULE_KEY,
        render: createReportRender('28')
    });
    // === LANGUAGE CATEGORY ===

    // Language category (not selectable)
    registry.add('adminRoute', 'contentReportReact-language', {
        isSelectable: false,
        targets: ['jcontent-jcontent-contentReports'],
        label: 'contentReportReact:categories.languages',
        requireModuleInstalledOnSite: MODULE_KEY
    });

    // Pages Without Title
    registry.add('adminRoute', 'contentReportReact-language-pagesWithoutTitle', {
        targets: ['jcontent-contentReportReact-language'],
        isSelectable: true,
        label: 'contentReportReact:menu.pagesWithoutTitle',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('10')
    });

    // Pages Untranslated
    registry.add('adminRoute', 'contentReportReact-language-pagesUntranslated', {
        targets: ['jcontent-contentReportReact-language'],
        isSelectable: true,
        label: 'contentReportReact:menu.pagesUntranslated',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('21')
    });

    // === VISIBILITY CONDITIONS CATEGORY ===

    // Visibility Conditions category (not selectable)
    registry.add('adminRoute', 'contentReportReact-visibility-conditions', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.visibility',
        requireModuleInstalledOnSite: MODULE_KEY
    });

    // Live Contents
    registry.add('adminRoute', 'contentReportReact-visibility-conditions-liveContents', {
        targets: ['jcontent-contentReportReact-visibility-conditions'],
        isSelectable: true,
        label: 'contentReportReact:menu.liveContents',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('25')
    });

    // Expired Contents
    registry.add('adminRoute', 'contentReportReact-visibility-conditions-expiredContents', {
        targets: ['jcontent-contentReportReact-visibility-conditions'],
        isSelectable: true,
        label: 'contentReportReact:menu.expiredContents',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('26')
    });

    // Future Contents
    registry.add('adminRoute', 'contentReportReact-visibility-conditions-futureContents', {
        targets: ['jcontent-contentReportReact-visibility-conditions'],
        isSelectable: true,
        label: 'contentReportReact:menu.futureContents',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('27')
    });

    // Displayed in some languages only
    addReportRoute('visibility-conditions', 'partialLanguageVisibility', '31');

    // === METADATA CATEGORY ===

    // Metadata category (not selectable)
    registry.add('adminRoute', 'contentReportReact-metadata', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.metadata',
        requireModuleInstalledOnSite: MODULE_KEY
    });

    // Pages Without Keywords
    registry.add('adminRoute', 'contentReportReact-metadata-pagesWithoutKeywords', {
        targets: ['jcontent-contentReportReact-metadata'],
        isSelectable: true,
        label: 'contentReportReact:menu.pagesWithoutKeywords',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('11')
    });

    // Pages Without Description
    registry.add('adminRoute', 'contentReportReact-metadata-pagesWithoutDescription', {
        targets: ['jcontent-contentReportReact-metadata'],
        isSelectable: true,
        label: 'contentReportReact:menu.pagesWithoutDescription',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('12')
    });

    // === USERS & GROUPS CATEGORY ===

    registry.add('adminRoute', 'contentReportReact-users-groups', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.usersGroups',
        requireModuleInstalledOnSite: MODULE_KEY,
        // Server-level user administration: the report lists every user of the platform
        requiredPermission: 'adminUsers'
    });

    registry.add('adminRoute', 'contentReportReact-users-groups-report', {
        targets: ['jcontent-contentReportReact-users-groups'],
        isSelectable: true,
        label: 'contentReportReact:menu.usersGroups',
        requireModuleInstalledOnSite: MODULE_KEY,
        // Server-level user administration: the report lists every user of the platform
        requiredPermission: 'adminUsers',

        render: createReportRender('usersGroups')
    });

    // === SYSTEM CATEGORY ===

    // System category (not selectable)
    registry.add('adminRoute', 'contentReportReact-system', {
        targets: ['jcontent-jcontent-contentReports'],
        isSelectable: false,
        label: 'contentReportReact:categories.system',
        requireModuleInstalledOnSite: MODULE_KEY
    });

    // Locked Content
    registry.add('adminRoute', 'contentReportReact-system-lockedContent', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.lockedContent',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('15')
    });

    // Custom Cache Content
    registry.add('adminRoute', 'contentReportReact-system-customCacheContent', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.customCacheContent',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('18')
    });

    // ACL Inheritance Break
    registry.add('adminRoute', 'contentReportReact-system-aclInheritanceBreak', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.aclInheritanceBreak',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('19')
    });

    // Modules deployed on site
    registry.add('adminRoute', 'contentReportReact-system-siteModules', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.siteModules',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('29')
    });

    // Component usage per module
    registry.add('adminRoute', 'contentReportReact-system-componentUsage', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.componentUsage',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('30')
    });

    // System Overview
    registry.add('adminRoute', 'contentReportReact-system-overview', {
        targets: ['jcontent-contentReportReact-system'],
        isSelectable: true,
        label: 'contentReportReact:menu.overview',
        requireModuleInstalledOnSite: MODULE_KEY,

        render: createReportRender('overview')
    });

    console.debug('%c contentReportReact jContent routes registered', 'color: #3c8cba');
};
