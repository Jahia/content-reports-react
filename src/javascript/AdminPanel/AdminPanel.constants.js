const DEFAULT_ROUTE = '/contentReportReact';

const PATH_DEFAULT = ({siteKey}) => (siteKey ? `/sites/${siteKey}` : '');
const FILES_PATH_DEFAULT = ({siteKey}) => (siteKey ? `/sites/${siteKey}/files` : '');

const reportCategories = [
    {id: 'overview', labelKey: 'categories.overview'},
    {id: 'content', labelKey: 'categories.content'},
    {id: 'references', labelKey: 'categories.references'},
    {id: 'languages', labelKey: 'categories.languages'},
    {id: 'visibility', labelKey: 'categories.visibility'},
    {id: 'metadata', labelKey: 'categories.metadata'},
    {id: 'usersGroups', labelKey: 'categories.usersGroups'},
    {id: 'system', labelKey: 'categories.system'}
];

const typeSearchOptions = [
    {value: 'pages', labelKey: 'fields.typeSearch.pages'},
    {value: 'content', labelKey: 'fields.typeSearch.content'}
];

const typeAuthorOptions = [
    {value: 'created', labelKey: 'fields.typeAuthor.created'},
    {value: 'modified', labelKey: 'fields.typeAuthor.modified'}
];

const typeDateOptions = [
    {value: 'created', labelKey: 'fields.typeDateSearch.created'},
    {value: 'modified', labelKey: 'fields.typeDateSearch.modified'}
];

const reports = [
    {
        id: 'overview',
        labelKey: 'menu.overview',
        descriptionKey: 'descriptions.overview',
        type: 'overview',
        category: 'overview',
        fields: []
    },
    {
        id: '20',
        labelKey: 'menu.byAuthorAndDate',
        descriptionKey: 'descriptions.byAuthorAndDate',
        type: 'legacy',
        category: 'content',
        fields: [
            // Position 0: Type search (pages or content) - must be sent to backend
            {name: 'typeSearch', type: 'radio', labelKey: 'fields.typeSearch.label', options: typeSearchOptions, defaultValue: 'pages', includeIfEmpty: true},
            // Position 1: Path (required, always sent)
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true, includeIfEmpty: true},
            // Position 2: Author search checkbox
            {name: 'searchAuthor', type: 'checkbox', labelKey: 'fields.searchByAuthor', defaultValue: false, includeIfEmpty: true},
            {
                name: 'typeAuthorSearch',
                type: 'radio',
                labelKey: 'fields.typeAuthorSearch',
                options: typeAuthorOptions,
                defaultValue: 'created',
                dependsOn: 'searchAuthor',
                dependsValue: true,
                uiOnly: true
            },
            // Position 3: Username to filter (only when searchAuthor=true)
            {
                name: 'searchUsername',
                type: 'userSelect',
                labelKey: 'fields.username',
                placeholderKey: 'fields.selectUser',
                dependsOn: 'searchAuthor',
                dependsValue: true,
                includeIfEmpty: true
            },
            // Position 4: Date search checkbox
            {name: 'searchByDate', type: 'checkbox', labelKey: 'fields.searchByDate', defaultValue: false, includeIfEmpty: true},
            {
                name: 'typeDateSearch',
                type: 'radio',
                labelKey: 'fields.typeDateSearch.label',
                options: typeDateOptions,
                defaultValue: 'created',
                dependsOn: 'searchByDate',
                dependsValue: true,
                uiOnly: true
            },
            // Position 5: Date begin (only when searchByDate=true)
            {name: 'dateBegin', type: 'date', labelKey: 'fields.dateBegin', dependsOn: 'searchByDate', dependsValue: true, includeIfEmpty: true},
            // Position 6: Date end (only when searchByDate=true) - CRITICAL: Must be within first 6, will need GraphQL extension
            {name: 'dateEnd', type: 'date', labelKey: 'fields.dateEnd', dependsOn: 'searchByDate', dependsValue: true, includeIfEmpty: true}
        ],
        columns: [
            {key: 'title', labelKey: 'reports.byAuthorAndDate.columns.title', sortable: true},
            {key: 'path', labelKey: 'reports.byAuthorAndDate.columns.path', sortable: true, type: 'link'},
            {key: 'type', labelKey: 'reports.byAuthorAndDate.columns.type', sortable: true},
            {key: 'created', labelKey: 'reports.byAuthorAndDate.columns.created', sortable: true, type: 'date'},
            {key: 'modified', labelKey: 'reports.byAuthorAndDate.columns.modified', sortable: true, type: 'date'},
            {key: 'published', labelKey: 'reports.byAuthorAndDate.columns.published', sortable: true, type: 'boolean'},
            {key: 'locked', labelKey: 'reports.byAuthorAndDate.columns.locked', sortable: true, type: 'boolean'}
        ]
    },
    {
        id: '1',
        labelKey: 'menu.byAuthor',
        descriptionKey: 'descriptions.byAuthor',
        type: 'legacy',
        fields: [
            {name: 'typeSearch', type: 'radio', labelKey: 'fields.typeSearch.label', options: typeSearchOptions, defaultValue: 'pages', includeIfEmpty: true},
            {name: 'typeAuthor', type: 'radio', labelKey: 'fields.typeAuthor.label', options: typeAuthorOptions, defaultValue: 'created'},
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true}
        ]
    },
    {
        id: '2',
        labelKey: 'menu.byAllDate',
        descriptionKey: 'descriptions.byAllDate',
        type: 'legacy',
        fields: [
            {name: 'typeAuthor', type: 'radio', labelKey: 'fields.typeAuthor.label', options: typeAuthorOptions, defaultValue: 'created'},
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true}
        ]
    },
    {
        id: '3',
        labelKey: 'menu.beforeDate',
        type: 'legacy',
        fields: [
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true},
            {name: 'date', type: 'date', labelKey: 'fields.date', required: true, includeIfEmpty: false}
        ]
    },
    {
        id: '4',
        labelKey: 'menu.byType',
        type: 'legacy',
        fields: [
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true}
        ]
    },
    {
        id: '5',
        labelKey: 'menu.byTypeDetailed',
        type: 'legacy',
        fields: [
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true}
        ]
    },
    {
        id: '6',
        labelKey: 'menu.byStatus',
        type: 'legacy',
        fields: [
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true}
        ]
    },
    {
        id: '7',
        labelKey: 'menu.byLanguage',
        type: 'legacy',
        fields: []
    },
    {
        id: '8',
        labelKey: 'menu.byLanguageDetailed',
        type: 'legacy',
        fields: [
            {name: 'reqLang', type: 'text', labelKey: 'fields.language', defaultValue: ({language}) => language || ''}
        ]
    },
    {
        id: '23',
        labelKey: 'menu.references',
        descriptionKey: 'descriptions.references',
        type: 'legacy',
        category: 'references',
        fields: [
            {name: 'pathTxtOrigin', type: 'path', labelKey: 'fields.originPath', defaultValue: PATH_DEFAULT, required: true},
            {name: 'pathTxtDestination', type: 'path', labelKey: 'fields.destinationPath', defaultValue: PATH_DEFAULT, required: true}
        ],
        columns: [
            {key: 'type', labelKey: 'reports.references.columns.type', sortable: true},
            {key: 'referencedPath', labelKey: 'reports.references.columns.referencedPath', sortable: true},
            {key: 'referencePath', labelKey: 'reports.references.columns.referencePath', sortable: true},
            {key: 'lastModified', labelKey: 'reports.references.columns.lastModified', sortable: true, type: 'date'},
            {key: 'link', labelKey: 'reports.references.columns.link', sortable: false, type: 'link'}
        ]
    },
    {
        id: '28',
        labelKey: 'menu.unusedAssets',
        descriptionKey: 'descriptions.unusedAssets',
        type: 'legacy',
        category: 'references',
        fields: [
            {name: 'assetsRootPath', type: 'path', labelKey: 'fields.assetsRootPath', defaultValue: FILES_PATH_DEFAULT, required: true}
        ],
        columns: [
            {key: 'mimeType', labelKey: 'reports.unusedAssets.columns.mimeType', sortable: false, type: 'icon'},
            {key: 'name', labelKey: 'reports.unusedAssets.columns.name', sortable: true},
            {key: 'path', labelKey: 'reports.unusedAssets.columns.path', sortable: true, type: 'link'},
            {key: 'creator', labelKey: 'reports.unusedAssets.columns.creator', sortable: true},
            {key: 'created', labelKey: 'reports.unusedAssets.columns.created', sortable: true, type: 'date'},
            {key: 'lastModified', labelKey: 'reports.unusedAssets.columns.lastModified', sortable: true, type: 'date'}
        ]
    },
    {
        id: '22',
        labelKey: 'menu.wipContent',
        descriptionKey: 'descriptions.wipContent',
        type: 'legacy',
        category: 'content',
        fields: [
            {name: 'typeSearch', type: 'radio', labelKey: 'fields.typeSearch.label', options: typeSearchOptions, defaultValue: 'pages', includeIfEmpty: true},
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true}
        ],
        columns: [
            {key: 'title', labelKey: 'reports.wipContent.columns.title', sortable: true},
            {key: 'type', labelKey: 'reports.wipContent.columns.type', sortable: true},
            {key: 'wipStatus', labelKey: 'reports.wipContent.columns.wipStatus', sortable: true},
            {key: 'path', labelKey: 'reports.wipContent.columns.path', sortable: true},
            {key: 'page', labelKey: 'reports.wipContent.columns.page', sortable: true, type: 'link'}
        ]
    },
    {
        id: '16',
        labelKey: 'menu.contentWaitingPublication',
        descriptionKey: 'descriptions.contentWaitingPublication',
        type: 'legacy',
        category: 'content',
        fields: [],
        columns: [
            {key: 'title', labelKey: 'reports.contentWaitingPublication.columns.title', sortable: true},
            {key: 'type', labelKey: 'reports.contentWaitingPublication.columns.type', sortable: true},
            {key: 'path', labelKey: 'reports.contentWaitingPublication.columns.path', sortable: true},
            {key: 'workflowStarted', labelKey: 'reports.contentWaitingPublication.columns.workflowStarted', sortable: true, type: 'date'},
            {key: 'workflowName', labelKey: 'reports.contentWaitingPublication.columns.workflowName', sortable: true},
            {key: 'workflowType', labelKey: 'reports.contentWaitingPublication.columns.workflowType', sortable: true}
        ]
    },
    {
        id: '24',
        labelKey: 'menu.markedForDeletion',
        type: 'legacy',
        category: 'content',
        descriptionKey: 'descriptions.markedForDeletion',
        fields: [
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true},
            {name: 'typeSearch', type: 'radio', labelKey: 'fields.typeSearch.label', options: typeSearchOptions, defaultValue: 'pages', includeIfEmpty: true}
        ],
        columns: [
            {key: 'nodeDisplayableName', labelKey: 'reports.markedForDeletion.columns.title', sortable: true},
            {key: 'nodeType', labelKey: 'reports.markedForDeletion.columns.type', sortable: true},
            {key: 'nodePath', labelKey: 'reports.markedForDeletion.columns.path', sortable: true},
            {key: 'nodeUsedInPagePath', labelKey: 'reports.markedForDeletion.columns.page', sortable: true, type: 'link'},
            {key: 'subNodesMarkedForDeletion', labelKey: 'reports.markedForDeletion.columns.subNodesDeleted', sortable: true},
            {key: 'publishStatus', labelKey: 'reports.markedForDeletion.columns.published', sortable: true}
        ]
    },
    {
        id: '10',
        labelKey: 'menu.pagesWithoutTitle',
        type: 'i18n',
        category: 'languages',
        descriptionKey: 'descriptions.pagesWithoutTitle',
        fields: []
    },
    {
        id: '21',
        labelKey: 'menu.pagesUntranslated',
        type: 'legacy',
        category: 'languages',
        descriptionKey: 'descriptions.pagesUntranslated',
        fields: [
            {name: 'selectLanguageBU', type: 'languageSelect', labelKey: 'fields.language', defaultValue: ({language}) => language || '', required: true},
            {name: 'selectTypeSearch', type: 'radio', labelKey: 'fields.typeSearch.label', options: typeSearchOptions, defaultValue: 'pages', includeIfEmpty: true},
            {name: 'pathTxt', type: 'path', labelKey: 'fields.path', defaultValue: PATH_DEFAULT, required: true}
        ],
        columns: [
            {key: 'title', labelKey: 'reports.pagesUntranslated.columns.title', sortable: true},
            {key: 'type', labelKey: 'reports.pagesUntranslated.columns.type', sortable: true},
            {key: 'path', labelKey: 'reports.pagesUntranslated.columns.path', sortable: true, type: 'link'},
            {key: 'date', labelKey: 'reports.pagesUntranslated.columns.date', sortable: true, type: 'date'}
        ]
    },
    {
        id: '25',
        labelKey: 'menu.liveContents',
        type: 'legacy',
        category: 'visibility',
        descriptionKey: 'descriptions.liveContents',
        fields: [
            {name: 'searchPath', type: 'path', labelKey: 'fields.searchPath', defaultValue: PATH_DEFAULT, required: true}
        ],
        columns: [
            {key: 'name', labelKey: 'reports.liveContents.columns.name', sortable: true},
            {key: 'path', labelKey: 'reports.liveContents.columns.path', sortable: true, type: 'link'},
            {key: 'type', labelKey: 'reports.liveContents.columns.type', sortable: true, type: 'html'},
            {key: 'listOfConditions', labelKey: 'reports.liveContents.columns.listOfConditions', sortable: true, type: 'html', noWrap: false},
            {key: 'isConditionMatched', labelKey: 'reports.liveContents.columns.isConditionMatched', sortable: true},
            {key: 'currentStatus', labelKey: 'reports.liveContents.columns.currentStatus', sortable: true}
        ]
    },
    {
        id: '26',
        labelKey: 'menu.expiredContents',
        type: 'legacy',
        category: 'visibility',
        descriptionKey: 'descriptions.expiredContents',
        fields: [
            {name: 'searchPath', type: 'path', labelKey: 'fields.searchPath', defaultValue: PATH_DEFAULT, required: true}
        ],
        columns: [
            {key: 'name', labelKey: 'reports.expiredContents.columns.name', sortable: true},
            {key: 'path', labelKey: 'reports.expiredContents.columns.path', sortable: true, type: 'link'},
            {key: 'type', labelKey: 'reports.expiredContents.columns.type', sortable: true, type: 'html'},
            {key: 'expiresOn', labelKey: 'reports.expiredContents.columns.expiresOn', sortable: true, type: 'date'}
        ]
    },
    {
        id: '27',
        labelKey: 'menu.futureContents',
        type: 'legacy',
        category: 'visibility',
        descriptionKey: 'descriptions.futureContents',
        fields: [
            {name: 'searchPath', type: 'path', labelKey: 'fields.searchPath', defaultValue: PATH_DEFAULT, required: true}
        ],
        columns: [
            {key: 'name', labelKey: 'reports.futureContents.columns.name', sortable: true},
            {key: 'path', labelKey: 'reports.futureContents.columns.path', sortable: true, type: 'link'},
            {key: 'type', labelKey: 'reports.futureContents.columns.type', sortable: true, type: 'html'},
            {key: 'liveDate', labelKey: 'reports.futureContents.columns.liveDate', sortable: true, type: 'date'}
        ]
    },
    {
        id: '31',
        labelKey: 'menu.partialLanguageVisibility',
        type: 'legacy',
        category: 'visibility',
        descriptionKey: 'descriptions.partialLanguageVisibility',
        fields: [
            {name: 'searchPath', type: 'path', labelKey: 'fields.searchPath', defaultValue: PATH_DEFAULT, required: true}
        ],
        // Every column sortable, labelled from reports.partialLanguageVisibility.columns.<key>
        columns: [{key: 'title'}, {key: 'path', type: 'link'}, {key: 'type'}, {key: 'displayedIn'}, {key: 'hiddenIn'}]
            .map(column => ({...column, labelKey: `reports.partialLanguageVisibility.columns.${column.key}`, sortable: true}))
    },
    {
        id: '11',
        labelKey: 'menu.pagesWithoutKeywords',
        type: 'legacy',
        category: 'metadata',
        descriptionKey: 'descriptions.pagesWithoutKeywords',
        fields: [],
        columns: [
            {key: 'nodeTitle', labelKey: 'reports.pagesWithoutKeywords.columns.title', sortable: true},
            {key: 'nodePath', labelKey: 'reports.pagesWithoutKeywords.columns.pagePath', sortable: true, type: 'link'}
        ]
    },
    {
        id: '12',
        labelKey: 'menu.pagesWithoutDescription',
        type: 'i18n',
        category: 'metadata',
        descriptionKey: 'descriptions.pagesWithoutDescription',
        fields: [],
        columns: [
            {key: 'path', labelKey: 'reports.pagesWithoutDescription.columns.path', sortable: true, type: 'link'}
        ]
    },
    {
        id: '15',
        labelKey: 'menu.lockedContent',
        type: 'legacy',
        category: 'system',
        descriptionKey: 'descriptions.lockedContent',
        fields: [],
        columns: [
            {key: 'nodeName', labelKey: 'reports.lockedContent.columns.title', sortable: true},
            {key: 'nodeType', labelKey: 'reports.lockedContent.columns.type', sortable: true, type: 'html'},
            {key: 'nodeAuthor', labelKey: 'reports.lockedContent.columns.author', sortable: true},
            {key: 'nodeLockedBy', labelKey: 'reports.lockedContent.columns.lockedBy', sortable: true},
            {key: 'nodeUsedInPagePath', labelKey: 'reports.lockedContent.columns.path', sortable: true, type: 'link'}
        ]
    },
    {
        id: '18',
        labelKey: 'menu.customCacheContent',
        type: 'legacy',
        category: 'system',
        descriptionKey: 'descriptions.customCacheContent',
        fields: [],
        columns: [
            {key: 'nodeName', labelKey: 'reports.customCacheContent.columns.title', sortable: true},
            {key: 'nodeType', labelKey: 'reports.customCacheContent.columns.type', sortable: true, type: 'html'},
            {key: 'expiration', labelKey: 'reports.customCacheContent.columns.expiration', sortable: true},
            {key: 'nodeUsedInPagePath', labelKey: 'reports.customCacheContent.columns.path', sortable: true, type: 'link'}
        ]
    },
    {
        id: '19',
        labelKey: 'menu.aclInheritanceBreak',
        type: 'legacy',
        category: 'system',
        descriptionKey: 'descriptions.aclInheritanceBreak',
        fields: [],
        columns: [
            {key: 'nodeName', labelKey: 'reports.aclInheritanceBreak.columns.title', sortable: true},
            {key: 'nodePath', labelKey: 'reports.aclInheritanceBreak.columns.path', sortable: true, type: 'link'}
        ]
    },
    {
        id: '29',
        labelKey: 'menu.siteModules',
        descriptionKey: 'descriptions.siteModules',
        type: 'legacy',
        category: 'system',
        fields: [],
        columns: [
            {key: 'id', labelKey: 'reports.siteModules.columns.id', sortable: true},
            {key: 'name', labelKey: 'reports.siteModules.columns.name', sortable: true},
            {key: 'version', labelKey: 'reports.siteModules.columns.version', sortable: true},
            {key: 'type', labelKey: 'reports.siteModules.columns.type', sortable: true},
            {key: 'dependency', labelKey: 'reports.siteModules.columns.dependency', sortable: true},
            {key: 'state', labelKey: 'reports.siteModules.columns.state', sortable: true},
            {key: 'components', labelKey: 'reports.siteModules.columns.components', sortable: true, type: 'number'}
        ]
    },
    {
        id: '30',
        labelKey: 'menu.componentUsage',
        descriptionKey: 'descriptions.componentUsage',
        type: 'legacy',
        category: 'system',
        fields: [],
        columns: [
            {key: 'module', labelKey: 'reports.componentUsage.columns.module', sortable: true},
            {key: 'moduleName', labelKey: 'reports.componentUsage.columns.moduleName', sortable: true},
            {key: 'component', labelKey: 'reports.componentUsage.columns.component', sortable: true},
            {key: 'label', labelKey: 'reports.componentUsage.columns.label', sortable: true},
            {key: 'usage', labelKey: 'reports.componentUsage.columns.usage', sortable: true, type: 'number'}
        ]
    },
    {
        id: 'usersGroups',
        labelKey: 'menu.usersGroups',
        descriptionKey: 'descriptions.usersGroups',
        type: 'usersGroups',
        category: 'usersGroups',
        fields: []
    }
];

export const buildReportsConfig = () => reports.map(report => ({
    ...report,
    pathDefault: PATH_DEFAULT
}));

export {DEFAULT_ROUTE, reportCategories};
