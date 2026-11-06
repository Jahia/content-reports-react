import React, {useCallback, useEffect, useMemo, useState} from 'react';
import PropTypes from 'prop-types';
import {Header, LayoutContent, Paper, Typography, Button, Input, Dropdown, CheckboxItem, RadioGroup, RadioItem} from '@jahia/moonstone';
import {useTranslation} from 'react-i18next';
import axios from 'axios';
import {buildReportsConfig} from './AdminPanel.constants';
import ReportResultsTable from './reports/ReportResultsTable';
import ContentActivityTable from './reports/ContentActivityTable';
import UsersGroupsReport from './reports/UsersGroupsReport';
import styles from './AdminPanel.module.scss';
import {OVERVIEW_QUERY, RAW_REPORT_QUERY, GET_SITE_LANGUAGES_QUERY, GET_ALL_USERS_QUERY} from '../graphql/queries';

const overviewStyles = {
    grid: {display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginTop: '16px'},
    card: {padding: '16px', borderRadius: '6px', background: '#ffffff', border: '1px solid #e1e7f5', boxShadow: '0 1px 2px rgba(0,0,0,0.05)'},
    value: {fontSize: '26px', fontWeight: '700', color: '#0c2f6b', marginTop: '6px'}
};

const buildInitialFields = (report, context) => {
    if (report && report.fields) {
        return report.fields.reduce((acc, field) => {
            const defaultValue = typeof field.defaultValue === 'function' ? field.defaultValue(context) : field.defaultValue;
            if (field.type === 'checkbox') {
                acc[field.name] = defaultValue === undefined ? false : Boolean(defaultValue);
            } else {
                acc[field.name] = defaultValue === undefined ? '' : defaultValue;
            }

            return acc;
        }, {});
    }

    return {};
};

const isFieldActive = (field, values) => {
    if (field.dependsOn) {
        const dependencyValue = values[field.dependsOn];
        if (field.dependsValue !== undefined) {
            return dependencyValue === field.dependsValue;
        }

        return Boolean(dependencyValue);
    }

    return true;
};

const buildParameterPayload = (fields, definitions) => {
    const entries = [];

    console.log('[buildParameterPayload] Input fields:', fields);
    console.log('[buildParameterPayload] Definitions:', definitions);

    (definitions || []).forEach(field => {
        if (!isFieldActive(field, fields)) {
            console.log(`[buildParameterPayload] Field ${field.name} is not active, skipping`);
            return;
        }

        // Skip UI-only fields
        if (field.uiOnly) {
            console.log(`[buildParameterPayload] Field ${field.name} is UI-only, skipping`);
            return;
        }

        const value = fields[field.name];
        console.log(`[buildParameterPayload] Processing field ${field.name}, value:`, value, 'type:', field.type);

        if (field.type === 'checkbox') {
            entries.push({name: field.name, value: value ? 'true' : 'false'});
            console.log(`[buildParameterPayload] Added checkbox ${field.name} = ${value ? 'true' : 'false'}`);
            return;
        }

        if (value === undefined || value === null || `${value}`.trim() === '') {
            if (field.includeIfEmpty) {
                entries.push({name: field.name, value: ''});
                console.log(`[buildParameterPayload] Added empty field ${field.name} (includeIfEmpty=true)`);
            } else {
                console.log(`[buildParameterPayload] Skipping empty field ${field.name} (includeIfEmpty=false)`);
            }

            return;
        }

        entries.push({name: field.name, value: `${value}`});
        console.log(`[buildParameterPayload] Added field ${field.name} = ${value}`);
    });

    console.log('[buildParameterPayload] Final entries:', entries);
    return entries;
};

const validateFields = (definitions, values, t) => {
    for (const field of definitions || []) {
        if (!isFieldActive(field, values)) {
            continue;
        }

        const required = field.required || (typeof field.requiredWhen === 'function' && field.requiredWhen(values));
        if (!required) {
            continue;
        }

        if (field.type === 'checkbox') {
            continue;
        }

        const value = values[field.name];
        if (value === undefined || value === null || `${value}`.trim() === '') {
            return t('errors.requiredField', {field: t(field.labelKey)});
        }
    }

    return null;
};

const tryParseJSON = value => {
    if (typeof value === 'string') {
        try {
            const parsed = JSON.parse(value);

            // Normalize different backend response formats
            // Some reports return {totalContent, items: [...]} instead of {recordsTotal, data: [...]}
            if (parsed && parsed.items && Array.isArray(parsed.items)) {
                // Convert items array of objects to array of arrays for table rendering
                const data = parsed.items.map(item => {
                    // Extract values in a consistent order based on keys
                    return [item.title, item.type, item.path, item.date];
                });

                return {
                    recordsTotal: parsed.totalContent || data.length,
                    recordsFiltered: parsed.totalContent || data.length,
                    data: data
                };
            }

            return parsed;
        } catch (_) {
            return null;
        }
    }

    return value;
};

const OverviewResult = ({data, labelKey, descriptionKey, siteKey, language, isLoading, lastUpdated, t}) => {
    const metrics = data ? [
        {label: t('result.nbPages'), value: data.nbPages},
        {label: t('result.nbTemplates'), value: data.nbTemplates},
        {label: t('result.nbUsers'), value: data.nbUsers},
        {label: t('result.nbContents'), value: data.nbContents},
        {label: t('result.nbEditorialContents'), value: data.nbEditorialContents},
        {label: t('result.nbWorkflowTasks'), value: data.nbWorkflowTasks},
        {label: t('result.nbFiles'), value: data.nbFiles},
        {label: t('result.nbImages'), value: data.nbImages}
    ] : [];

    // Function to get flag emoji from language code
    const getFlagEmoji = langCode => {
        const flagMap = {
            en: '🇬🇧',
            fr: '🇫🇷',
            de: '🇩🇪',
            es: '🇪🇸',
            it: '🇮🇹',
            pt: '🇵🇹',
            nl: '🇳🇱',
            ru: '🇷🇺',
            ja: '🇯🇵',
            zh: '🇨🇳',
            ar: '🇸🇦',
            ko: '🇰🇷',
            pl: '🇵🇱',
            tr: '🇹🇷',
            sv: '🇸🇪',
            da: '🇩🇰',
            no: '🇳🇴',
            fi: '🇫🇮',
            cs: '🇨🇿',
            el: '🇬🇷'
        };
        return flagMap[langCode] || '🌐';
    };

    return (
        <Paper style={{padding: '32px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04)'}}>
            <div style={{paddingBottom: '20px', borderBottom: '1px solid #e8ecf0', marginBottom: '24px'}}>
                <Typography variant="heading" weight="bold" style={{fontSize: '20px', color: '#1a2332', marginBottom: '6px', lineHeight: '1.4'}}>
                    {t(labelKey)}
                </Typography>
                {descriptionKey && (
                    <Typography variant="body" style={{color: '#5c6f90', fontSize: '14px', lineHeight: '1.6'}}>
                        {t(descriptionKey)}
                    </Typography>
                )}
            </div>
            {data && (
                <div>
                    <div style={{color: '#2c3e5d', fontWeight: '600', fontSize: '16px', marginBottom: '16px'}}>{data.siteDisplayableName || data.siteName}</div>
                    <div style={overviewStyles.grid}>
                        {metrics.map(item => (
                            <div key={item.label} style={overviewStyles.card}>
                                <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500'}}>{item.label}</div>
                                <div style={overviewStyles.value}>{item.value}</div>
                            </div>
                        ))}
                        {data.languages && data.languages.length > 0 && (
                            <div style={{...overviewStyles.card, gridColumn: 'span 2'}}>
                                <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500', marginBottom: '8px'}}>
                                    {t('result.languages')} ({data.nbLanguages})
                                </div>
                                <div style={{display: 'flex', flexWrap: 'wrap', gap: '8px'}}>
                                    {data.languages.map(lang => (
                                        <div
                                            key={lang}
                                            style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                gap: '6px',
                                                padding: '6px 12px',
                                                backgroundColor: '#f4f6f9',
                                                borderRadius: '6px',
                                                fontSize: '14px',
                                                fontWeight: '500',
                                                color: '#2c3e5d'
                                            }}
                                        >
                                            <span style={{fontSize: '18px'}}>{getFlagEmoji(lang)}</span>
                                            <span>{lang.toUpperCase()}</span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Content Activity Section */}
                    <div style={{marginTop: '32px', paddingTop: '24px', borderTop: '2px solid #e8ecf0'}}>
                        <Typography variant="heading" weight="bold" style={{fontSize: '18px', color: '#1a2332', marginBottom: '16px'}}>
                            {t('result.contentActivity')}
                        </Typography>

                        {/* Activity Metrics Grid */}
                        <div style={overviewStyles.grid}>
                            <div style={overviewStyles.card}>
                                <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500'}}>{t('result.newContentLast30Days')}</div>
                                <div style={{...overviewStyles.value, color: '#22863a'}}>{data.newContentLast30Days}</div>
                            </div>
                            <div style={overviewStyles.card}>
                                <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500'}}>{t('result.modifiedContentLast30Days')}</div>
                                <div style={{...overviewStyles.value, color: '#0366d6'}}>{data.modifiedContentLast30Days}</div>
                            </div>
                            <div style={overviewStyles.card}>
                                <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500'}}>{t('result.publishedContentLast30Days')}</div>
                                <div style={{...overviewStyles.value, color: '#6f42c1'}}>{data.publishedContentLast30Days}</div>
                            </div>
                            <div style={overviewStyles.card}>
                                <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500'}}>{t('result.publishedNodes')}</div>
                                <div style={overviewStyles.value}>{data.publishedNodes}</div>
                            </div>
                            <div style={overviewStyles.card}>
                                <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500'}}>{t('result.unpublishedNodes')}</div>
                                <div style={overviewStyles.value}>{data.unpublishedNodes}</div>
                            </div>
                            <div style={overviewStyles.card}>
                                <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500'}}>{t('result.averageTimeToPublish')}</div>
                                <div style={overviewStyles.value}>{data.averageTimeToPublish?.toFixed(1)} {t('result.days')}</div>
                            </div>
                        </div>

                        {/* Top Contributors */}
                        <div style={{...overviewStyles.card, marginTop: '16px'}}>
                            <div style={{color: '#5c6f90', fontSize: '13px', fontWeight: '500', marginBottom: '12px'}}>
                                {t('result.topContributors')}
                            </div>
                            {data.topContributors && data.topContributors.length > 0 ? (
                                <div style={{display: 'flex', flexDirection: 'column', gap: '8px'}}>
                                    {data.topContributors.map((contributor, index) => (
                                        <div
                                            key={contributor.username}
                                            style={{
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                alignItems: 'center',
                                                padding: '8px 12px',
                                                backgroundColor: '#f4f6f9',
                                                borderRadius: '4px'
                                            }}
                                        >
                                            <div style={{display: 'flex', alignItems: 'center', gap: '8px'}}>
                                                <span style={{
                                                    display: 'inline-flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    width: '24px',
                                                    height: '24px',
                                                    borderRadius: '50%',
                                                    backgroundColor: index === 0 ? '#ffd700' : index === 1 ? '#c0c0c0' : index === 2 ? '#cd7f32' : '#e8ecf0',
                                                    fontSize: '12px',
                                                    fontWeight: 'bold',
                                                    color: index < 3 ? '#fff' : '#5c6f90'
                                                }}
                                                >
                                                    {index + 1}
                                                </span>
                                                <span style={{fontWeight: '500', color: '#2c3e5d'}}>{contributor.username}</span>
                                            </div>
                                            <span style={{
                                                fontSize: '14px',
                                                fontWeight: '600',
                                                color: '#0c2f6b',
                                                backgroundColor: '#e1e7f5',
                                                padding: '4px 8px',
                                                borderRadius: '4px'
                                            }}
                                            >
                                                {contributor.contentCount} {t('result.items')}
                                            </span>
                                        </div>
                                    ))}
                                </div>
                                ) : (
                                    <div style={{color: '#8b9bb3', fontSize: '13px', fontStyle: 'italic', padding: '12px'}}>
                                        {t('result.noContributorsData')}
                                    </div>
                                )}
                        </div>

                        {/* Detailed list of everything touched in the window */}
                        <ContentActivityTable
                            activity={data.recentActivity}
                            total={data.recentActivityTotal}
                            siteKey={siteKey}
                            language={language}
                            t={t}
                        />
                    </div>
                </div>
            )}
            <div style={{paddingTop: '20px', borderTop: '1px solid #e8ecf0'}}>
                <FormActions isLoading={isLoading} lastUpdated={lastUpdated} t={t}/>
            </div>
        </Paper>
    );
};

OverviewResult.propTypes = {
    data: PropTypes.shape({
        nbPages: PropTypes.number,
        nbTemplates: PropTypes.number,
        nbUsers: PropTypes.number,
        nbContents: PropTypes.number,
        nbEditorialContents: PropTypes.number,
        nbWorkflowTasks: PropTypes.number,
        nbFiles: PropTypes.number,
        nbImages: PropTypes.number,
        languages: PropTypes.arrayOf(PropTypes.string),
        nbLanguages: PropTypes.number,
        siteDisplayableName: PropTypes.string,
        siteName: PropTypes.string,
        newContentLast30Days: PropTypes.number,
        modifiedContentLast30Days: PropTypes.number,
        publishedContentLast30Days: PropTypes.number,
        unpublishedNodes: PropTypes.number,
        publishedNodes: PropTypes.number,
        averageTimeToPublish: PropTypes.number,
        topContributors: PropTypes.arrayOf(PropTypes.shape({
            username: PropTypes.string,
            contentCount: PropTypes.number
        })),
        recentActivityTotal: PropTypes.number,
        recentActivity: PropTypes.arrayOf(PropTypes.object)
    }),
    labelKey: PropTypes.string.isRequired,
    descriptionKey: PropTypes.string,
    siteKey: PropTypes.string,
    language: PropTypes.string,
    isLoading: PropTypes.bool.isRequired,
    lastUpdated: PropTypes.instanceOf(Date),
    t: PropTypes.func.isRequired
};

const RawResult = ({result, siteKey, language, labelKey, selectedReport, t}) => {
    if (!result) {
        return null;
    }

    const parsed = result.parsed;

    return (
        <Paper style={{padding: '32px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04)'}}>
            <div style={{paddingBottom: '20px', borderBottom: '1px solid #e8ecf0', marginBottom: '24px'}}>
                <Typography variant="heading" weight="bold" style={{fontSize: '20px', color: '#1a2332', lineHeight: '1.4'}}>
                    {t('common.report')} - {t(labelKey)}
                </Typography>
            </div>
            {parsed && parsed.data && (
                <ReportResultsTable
                    data={parsed}
                    siteKey={siteKey}
                    language={language}
                    columns={selectedReport?.columns}
                    reportId={selectedReport?.id}
                    reportType={selectedReport?.type}
                />
            )}
        </Paper>
    );
};

RawResult.propTypes = {
    result: PropTypes.shape({
        payload: PropTypes.string,
        parsed: PropTypes.shape({
            recordsTotal: PropTypes.number,
            recordsFiltered: PropTypes.number,
            data: PropTypes.arrayOf(PropTypes.array)
        })
    }),
    siteKey: PropTypes.string.isRequired,
    language: PropTypes.string,
    labelKey: PropTypes.string.isRequired,
    selectedReport: PropTypes.object,
    t: PropTypes.func.isRequired
};

const FormActions = ({isLoading, lastUpdated, t}) => (
    <div className={styles.actions}>
        <Button
            type="submit"
            size="big"
            variant="outlined"
            label={isLoading ? t('states.loading') : t('actions.runReport')}
            disabled={isLoading}
        />
        {lastUpdated && (
            <span style={{color: '#4b5c87', fontSize: '13px'}}>
                {t('states.lastUpdated', {value: lastUpdated.toLocaleString()})}
            </span>
        )}
    </div>
);

FormActions.propTypes = {
    isLoading: PropTypes.bool.isRequired,
    lastUpdated: PropTypes.instanceOf(Date),
    t: PropTypes.func.isRequired
};

const ResultSection = ({error, isLoading, result, siteKey, language, report, t}) => (
    <>
        {error && (
            <div className={styles.error}>
                {error.message}
            </div>
        )}

        {!isLoading && result && result.kind === 'overview' && (
            <OverviewResult
                data={result.overview}
                labelKey={report.labelKey}
                descriptionKey={report.descriptionKey}
                siteKey={siteKey}
                language={language}
                t={t}
            />
        )}

        {!isLoading && result && result.kind === 'legacy' && (
            <RawResult
                result={result}
                siteKey={siteKey}
                language={language}
                labelKey={report.labelKey}
                selectedReport={report}
                t={t}
            />
        )}

        {isLoading && (
            <div className={styles.info}>{t('states.loading')}</div>
        )}
    </>
);

ResultSection.propTypes = {
    error: PropTypes.instanceOf(Error),
    isLoading: PropTypes.bool.isRequired,
    result: PropTypes.shape({
        kind: PropTypes.string,
        overview: PropTypes.object
    }),
    siteKey: PropTypes.string.isRequired,
    language: PropTypes.string,
    report: PropTypes.shape({
        labelKey: PropTypes.string.isRequired,
        descriptionKey: PropTypes.string
    }).isRequired,
    t: PropTypes.func.isRequired
};

// The site and language selected in jContent. contextJsParameters and jahiaGWTParameters hold the
// site the server resolved for the request (systemsite when sites share the server name
// localhost, as on Jahia Cloud), not the one the user is browsing.
const getJContentContext = () => {
    const state = window.jahia?.reduxStore?.getState?.() || {};
    const context = window.contextJsParameters || {};
    return {
        siteKey: state.site || context.siteKey || context.site?.key || '',
        language: state.language || context.lang || context.language || '',
        uilang: state.uilang || context.uilang || ''
    };
};

const AdminPanel = ({initialReportId}) => {
    const {t} = useTranslation('contentReportReact');

    const context = window.contextJsParameters || {};
    const {siteKey, language, uilang} = getJContentContext();
    const baseContentPath = useMemo(() => (siteKey ? `/sites/${siteKey}` : ''), [siteKey]);
    const graphqlEndpoint = `${context.contextPath || ''}/modules/graphql`;

    const reports = useMemo(() => buildReportsConfig(), []);
    const selectedReportId = initialReportId || reports[0]?.id;

    const selectedReport = useMemo(
        () => reports.find(report => report.id === selectedReportId) || reports[0],
        [reports, selectedReportId]
    );

    const [fields, setFields] = useState(() => buildInitialFields(selectedReport, {siteKey, language}));
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [result, setResult] = useState(null);
    const [lastUpdated, setLastUpdated] = useState(null);
    const [siteLanguages, setSiteLanguages] = useState([]);
    const [users, setUsers] = useState([]);
    const [loadingUsers, setLoadingUsers] = useState(false);

    const executeQuery = useCallback(async (query, variables) => {
        const response = await axios.post(graphqlEndpoint, {query, variables}, {
            headers: {'Content-Type': 'application/json'},
            withCredentials: true
        });

        if (response.data && response.data.errors && response.data.errors.length) {
            const message = response.data.errors.map(err => err.message).join('\n');
            throw new Error(message);
        }

        return response.data.data;
    }, [graphqlEndpoint]);

    // Fetch site languages on mount
    useEffect(() => {
        const fetchSiteLanguages = async () => {
            if (!siteKey) {
                return;
            }

            try {
                const data = await executeQuery(GET_SITE_LANGUAGES_QUERY, {siteKey: `/sites/${siteKey}`});
                const languagesData = data?.jcr?.nodeByPath?.site?.languages || [];
                const languages = languagesData.map(lang => lang.language);
                console.log('Fetched site languages:', languages);
                setSiteLanguages(languages);
            } catch (err) {
                console.error('Failed to fetch site languages:', err);
                // Fallback: try to get from context
                const contextLanguages = window.contextJsParameters?.siteLanguages || [];
                console.log('Using fallback languages from context:', contextLanguages);
                setSiteLanguages(contextLanguages);
            }
        };

        fetchSiteLanguages();
    }, [siteKey, executeQuery]);

    // Fetch all users on mount
    useEffect(() => {
        const fetchUsers = async () => {
            setLoadingUsers(true);
            try {
                const data = await executeQuery(GET_ALL_USERS_QUERY, {});
                const rawUsers = data?.jcr?.nodesByQuery?.nodes || [];
                const userList = rawUsers.map(user => ({
                    username: user.name,
                    email: user.property?.value || ''
                }));
                setUsers(userList);
            } catch (err) {
                console.error('Failed to fetch users:', err);
            } finally {
                setLoadingUsers(false);
            }
        };

        fetchUsers();
    }, [executeQuery]);

    useEffect(() => {
        setFields(buildInitialFields(selectedReport, {siteKey, language}));
        setResult(null);
        setError(null);
    }, [selectedReport, siteKey, language]);

    const handleFieldChange = useCallback((name, value) => {
        console.log('[AdminPanel] handleFieldChange - name:', name, 'value:', value);
        setFields(prev => {
            const newFields = {...prev, [name]: value};
            console.log('[AdminPanel] handleFieldChange - new fields state:', newFields);
            return newFields;
        });
    }, []);

    const runOverview = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await executeQuery(OVERVIEW_QUERY, {siteKey, language});
            const overview = data?.admin?.contentReports?.overview;
            setResult({kind: 'overview', overview});
            setLastUpdated(new Date());
        } catch (err) {
            setError(err);
        } finally {
            setLoading(false);
        }
    }, [executeQuery, siteKey, language]);

    const runLegacy = useCallback(async () => {
        if (!selectedReport) {
            return;
        }

        setLoading(true);
        setError(null);
        try {
            const parameters = buildParameterPayload(fields, selectedReport.fields);

            console.log('[AdminPanel] runLegacy - fields:', fields);
            console.log('[AdminPanel] runLegacy - parameters:', parameters);

            // Convert parameters array to individual name/value pairs (max 5 parameters supported)
            const variables = {
                siteKey,
                language,
                reportId: selectedReport.id,
                offset: selectedReport.defaultOffset ?? 0,
                limit: selectedReport.defaultLimit ?? 10000, // Fetch all results for client-side pagination
                sortColumn: selectedReport.defaultSortColumn ?? null,
                sortDirection: selectedReport.defaultSortDirection ?? null
            };

            // Add up to 6 parameters as individual fields
            parameters.forEach((param, index) => {
                if (index < 6) {
                    variables[`parameterName${index + 1}`] = param.name;
                    variables[`parameterValue${index + 1}`] = param.value;
                }
            });

            console.log('[AdminPanel] runLegacy - GraphQL variables:', variables);

            const data = await executeQuery(RAW_REPORT_QUERY, variables);
            const payload = data?.admin?.contentReports?.rawReport;
            const parsed = tryParseJSON(payload);
            setResult({kind: 'legacy', payload, parsed});
            setLastUpdated(new Date());
        } catch (err) {
            setError(err);
        } finally {
            setLoading(false);
        }
    }, [executeQuery, fields, language, selectedReport, siteKey]);

    const handleSubmit = useCallback(event => {
        event.preventDefault();
        if (!selectedReport) {
            return;
        }

        const validationError = validateFields(selectedReport.fields, fields, t);
        if (validationError) {
            setError(new Error(validationError));
            return;
        }

        if (selectedReport.type === 'overview') {
            runOverview();
        } else if (selectedReport.type !== 'usersGroups') {
            runLegacy();
        }
    }, [fields, runLegacy, runOverview, selectedReport, t]);

    useEffect(() => {
        if (selectedReport && selectedReport.type === 'overview') {
            runOverview();
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedReportId]);

    const openPathPicker = useCallback((fieldName, currentValue, pickerType = 'editorial') => {
        const initialPath = currentValue || baseContentPath || '/';
        const picker = window?.CE_API?.openPicker;

        console.log('[AdminPanel] openPathPicker - fieldName:', fieldName, 'currentValue:', currentValue, 'initialPath:', initialPath);

        if (picker) {
            picker({
                type: pickerType,
                initialSelectedItem: [initialPath],
                site: siteKey,
                lang: uilang || language || 'en',
                isMultiple: false,
                setValue: ([selected]) => {
                    console.log('[AdminPanel] Path picker returned:', selected);
                    if (selected?.path) {
                        console.log('[AdminPanel] Setting field', fieldName, 'to path:', selected.path);
                        handleFieldChange(fieldName, selected.path);
                    }
                }
            });
            return;
        }

        console.warn('[contentReportReact] Content Editor picker API is not available.');
    }, [baseContentPath, handleFieldChange, language, siteKey, uilang]);

    const getSelectOptions = useCallback(field => {
        if (field.type === 'languageSelect') {
            return {
                data: siteLanguages.map(lang => ({
                    label: lang,
                    value: lang
                })),
                isLoading: false
            };
        }

        if (field.type === 'userSelect') {
            return {
                data: users.map(user => ({
                    value: user.username,
                    label: `${user.username}${user.email ? ` (${user.email})` : ''}`
                })),
                isLoading: loadingUsers
            };
        }

        return {
            data: (field.options || []).map(option => ({
                value: option.value,
                label: t(option.labelKey)
            })),
            isLoading: false
        };
    }, [siteLanguages, users, loadingUsers, t]);

    const renderGenericField = useCallback(field => {
        if (!field || !isFieldActive(field, fields)) {
            return null;
        }

        if (field.type === 'checkbox') {
            return (
                <div key={field.name} className={styles.checkboxRow}>
                    <CheckboxItem
                        id={field.name}
                        value={field.name}
                        label={t(field.labelKey)}
                        checked={Boolean(fields[field.name])}
                        onChange={(event, value, checked) => handleFieldChange(field.name, checked)}
                    />
                </div>
            );
        }

        if (field.type === 'radio') {
            return (
                <div key={field.name} className={styles.formRow}>
                    <span className={styles.label}>{t(field.labelKey)}</span>
                    <RadioGroup
                        name={field.name}
                        value={fields[field.name]}
                        className={styles.radioGroup}
                        onChange={event => handleFieldChange(field.name, event.target.value)}
                    >
                        {(field.options || []).map(option => (
                            <RadioItem
                                key={`${field.name}-${option.value}`}
                                id={`${field.name}-${option.value}`}
                                value={option.value}
                                label={t(option.labelKey)}
                            />
                        ))}
                    </RadioGroup>
                </div>
            );
        }

        if (field.type === 'select' || field.type === 'languageSelect' || field.type === 'userSelect') {
            const {data, isLoading} = getSelectOptions(field);
            const placeholder = field.placeholderKey ? t(field.placeholderKey) : (t('common.selectOption') || 'Select...');

            return (
                <div key={field.name} className={styles.formRow}>
                    <label className={styles.label} htmlFor={field.name}>{t(field.labelKey)}</label>
                    <Dropdown
                        id={field.name}
                        data={data}
                        value={fields[field.name] ?? ''}
                        isDisabled={isLoading}
                        isLoading={isLoading}
                        placeholder={isLoading ? (t('common.loading') || 'Loading...') : placeholder}
                        variant="outlined"
                        size="medium"
                        className={styles.dropdownInput}
                        onChange={(event, item) => handleFieldChange(field.name, item.value)}
                    />
                </div>
            );
        }

        const isPathField = field.type === 'path';
        const inputType = field.type === 'date' ? 'date' : 'text';
        const placeholder = field.placeholderKey ? t(field.placeholderKey) : '';

        return (
            <div key={field.name} className={styles.formRow}>
                <label className={styles.label} htmlFor={field.name}>{t(field.labelKey)}</label>
                <div className={isPathField ? styles.inline : styles.inputRow}>
                    <Input
                        id={field.name}
                        type={inputType}
                        value={fields[field.name] ?? ''}
                        size="big"
                        placeholder={placeholder}
                        onChange={event => handleFieldChange(field.name, event.target.value)}
                    />
                    {isPathField && (
                        <Button
                            size="big"
                            variant="outlined"
                            label={t('actions.browse')}
                            onClick={() => openPathPicker(field.name, fields[field.name], selectedReport?.id === '28' && field.name === 'assetsRootPath' ? 'folder' : 'editorial')}
                        />
                    )}
                </div>
            </div>
        );
    }, [fields, handleFieldChange, openPathPicker, selectedReport?.id, t, getSelectOptions]);

    const renderFormFields = () => {
        if (!selectedReport || selectedReport.type === 'overview' || selectedReport.type === 'usersGroups') {
            return null;
        }

        // Render generic fields for all reports
        const genericFields = selectedReport.fields?.map(renderGenericField) || null;
        return genericFields ? (
            <Paper style={{padding: '32px', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04)', display: 'flex', flexDirection: 'column', gap: '28px'}}>
                <div style={{paddingBottom: '20px', borderBottom: '1px solid #e8ecf0'}}>
                    <Typography variant="heading" weight="bold" style={{fontSize: '20px', color: '#1a2332', marginBottom: '6px', lineHeight: '1.4'}}>
                        {t(selectedReport.labelKey)}
                    </Typography>
                    {selectedReport.descriptionKey && (
                        <Typography variant="body" style={{color: '#5c6f90', fontSize: '14px', lineHeight: '1.6'}}>
                            {t(selectedReport.descriptionKey)}
                        </Typography>
                    )}
                </div>
                {genericFields}
                <div style={{paddingTop: '20px', borderTop: '1px solid #e8ecf0'}}>
                    <FormActions isLoading={loading} lastUpdated={lastUpdated} t={t}/>
                </div>
            </Paper>
        ) : null;
    };

    return (
        <>
            <Header
                title={`${t('label')} ${siteKey ? `- ${siteKey}` : ''}`}
            />
            <LayoutContent
                content={(
                    <div className={styles.pageContainer}>
                        <div className={styles.contentFullWidth}>
                            {selectedReport?.type === 'overview' ? (
                                // For overview report: show result with button in paper
                                <form className={styles.form} onSubmit={handleSubmit}>
                                    <OverviewResult
                                        data={result?.overview}
                                        labelKey={selectedReport.labelKey}
                                        descriptionKey={selectedReport.descriptionKey}
                                        siteKey={siteKey}
                                        language={language}
                                        isLoading={loading}
                                        lastUpdated={lastUpdated}
                                        t={t}
                                    />
                                </form>
                            ) : selectedReport?.type === 'usersGroups' ? (
                                <UsersGroupsReport graphqlEndpoint={graphqlEndpoint}/>
                            ) : selectedReport ? (
                                <form className={styles.form} onSubmit={handleSubmit}>
                                    {renderFormFields()}
                                </form>
                            ) : (
                                <div className={styles.error}>
                                    {t('errors.noReportSelected')}
                                </div>
                            )}
                        </div>
                        <div className={styles.fullWidthResults}>
                            {selectedReport?.type !== 'overview' && selectedReport?.type !== 'usersGroups' && (
                                <ResultSection error={error} isLoading={loading} result={result} siteKey={siteKey} language={language} report={selectedReport} t={t}/>
                            )}
                        </div>
                    </div>
                )}
            />
        </>
    );
};

AdminPanel.displayName = 'ContentReportAdminPanel';

AdminPanel.propTypes = {
    initialReportId: PropTypes.string
};

export default AdminPanel;
