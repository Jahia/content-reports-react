package org.jahia.modules.contentreports.service;

import org.apache.commons.lang3.StringUtils;
import org.jahia.modules.contentreports.bean.BaseReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jahia.modules.contentreports.bean.ReportAclInheritanceStopped;
import org.jahia.modules.contentreports.bean.ReportByAllDate;
import org.jahia.modules.contentreports.bean.ReportByAuthor;
import org.jahia.modules.contentreports.bean.ReportBeforeDate;
import org.jahia.modules.contentreports.bean.ReportByDateAndAuthor;
import org.jahia.modules.contentreports.bean.ReportByExpiredContent;
import org.jahia.modules.contentreports.bean.ReportByFutureContent;
import org.jahia.modules.contentreports.bean.ReportByLanguage;
import org.jahia.modules.contentreports.bean.ReportByLanguageDetailed;
import org.jahia.modules.contentreports.bean.ReportByStatus;
import org.jahia.modules.contentreports.bean.ReportByType;
import org.jahia.modules.contentreports.bean.ReportByTypeDetailed;
import org.jahia.modules.contentreports.bean.ReportByUnstranslated;
import org.jahia.modules.contentreports.bean.ReportContentFromAnotherSite;
import org.jahia.modules.contentreports.bean.ReportContentMarkedForDeletion;
import org.jahia.modules.contentreports.bean.ReportContentWaitingPublication;
import org.jahia.modules.contentreports.bean.ReportCustomCacheContent;
import org.jahia.modules.contentreports.bean.ReportDisplayLinks;
import org.jahia.modules.contentreports.bean.ReportLiveContents;
import org.jahia.modules.contentreports.bean.ReportLockedContent;
import org.jahia.modules.contentreports.bean.ReportOrphanContent;
import org.jahia.modules.contentreports.bean.ReportOverview;
import org.jahia.modules.contentreports.bean.ReportSiteModules;
import org.jahia.modules.contentreports.bean.ReportComponentUsage;
import org.jahia.modules.contentreports.bean.ReportPagesWithoutDescription;
import org.jahia.modules.contentreports.bean.ReportPartialLanguageVisibility;
import org.jahia.modules.contentreports.bean.ReportPagesWithoutKeyword;
import org.jahia.modules.contentreports.bean.ReportPagesWithoutTitle;
import org.jahia.modules.contentreports.bean.ReportUnreferencedAssets;
import org.jahia.modules.contentreports.bean.ReportWipContent;
import org.jahia.modules.contentreports.exception.ContentReportException;
import org.jahia.services.content.decorator.JCRSiteNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class ContentReportFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentReportFactory.class);

    private static final String SEARCH_PATH = "searchPath";

    /**
     * Reports built from the site and, for some, a search path only. Kept out of the switch in
     * {@link #build}, which handles the reports that read several parameters.
     */
    private static final Map<String, BiFunction<JCRSiteNode, Map<String, String>, BaseReport>> SIMPLE_REPORTS = new HashMap<>();

    static {
        SIMPLE_REPORTS.put("25", (site, params) -> new ReportLiveContents(site, cleanPath(params.get(SEARCH_PATH))));
        SIMPLE_REPORTS.put("26", (site, params) -> new ReportByExpiredContent(site, cleanPath(params.get(SEARCH_PATH))));
        SIMPLE_REPORTS.put("27", (site, params) -> new ReportByFutureContent(site, cleanPath(params.get(SEARCH_PATH))));
        SIMPLE_REPORTS.put("29", (site, params) -> new ReportSiteModules(site));
        SIMPLE_REPORTS.put("30", (site, params) -> new ReportComponentUsage(site));
        SIMPLE_REPORTS.put("31", (site, params) -> new ReportPartialLanguageVisibility(site, cleanPath(params.get(SEARCH_PATH))));
    }

    private ContentReportFactory() {
    }

    public static BaseReport build(String reportId, JCRSiteNode siteNode, Map<String, String> parameters,
                                   Integer sortColumn, String sortDirection) throws ContentReportException {
        Map<String, String> params = parameters != null ? parameters : Collections.emptyMap();
        int sortCol = sortColumn != null ? sortColumn : parseInt(params.get("order[0][column]"), 0);
        String order = StringUtils.defaultString(sortDirection, params.getOrDefault("order[0][dir]", ""));

        BiFunction<JCRSiteNode, Map<String, String>, BaseReport> simpleReport = SIMPLE_REPORTS.get(reportId);
        if (simpleReport != null) {
            return simpleReport.apply(siteNode, params);
        }

        switch (reportId) {
            case "1":
                return new ReportByAuthor(
                        siteNode,
                        cleanPath(params.get("pathTxt")),
                        isPages(params.get("typeSearch")) ? BaseReport.SearchContentType.PAGE : BaseReport.SearchContentType.CONTENT,
                        isCreated(params.get("typeAuthor")) ? BaseReport.SearchActionType.CREATION : BaseReport.SearchActionType.UPDATE,
                        true
                );
            case "2":
                return new ReportByAllDate(
                        siteNode,
                        isCreated(params.get("typeAuthor")) ? BaseReport.SearchActionType.CREATION : BaseReport.SearchActionType.UPDATE,
                        cleanPath(params.get("pathTxt")),
                        true
                );
            case "3":
                return new ReportBeforeDate(siteNode, cleanPath(params.get("pathTxt")), params.get("date"), true);
            case "4":
                return new ReportByType(siteNode, cleanPath(params.get("pathTxt")));
            case "5":
                return new ReportByTypeDetailed(siteNode, cleanPath(params.get("pathTxt")));
            case "6":
                return new ReportByStatus(siteNode, cleanPath(params.get("pathTxt")));
            case "7":
                return new ReportByLanguage(siteNode);
            case "8":
                return new ReportByLanguageDetailed(siteNode, params.get("reqLang"));
            case "10":
                return new ReportPagesWithoutTitle(siteNode, params.get("language"));
            case "11":
                return new ReportPagesWithoutKeyword(siteNode, sortCol, order);
            case "12":
                return new ReportPagesWithoutDescription(siteNode, params.get("language"));
            case "13":
                return new ReportContentFromAnotherSite(siteNode);
            case "14":
                return new ReportOrphanContent(siteNode);
            case "15":
                return new ReportLockedContent(siteNode, sortCol, order);
            case "16":
                return new ReportContentWaitingPublication(siteNode);
            case "17":
                return new ReportOverview(siteNode);
            case "18":
                return new ReportCustomCacheContent(siteNode, sortCol, order);
            case "19":
                return new ReportAclInheritanceStopped(siteNode);
            case "20":
                LOGGER.debug("Building report 20 with params: {}", params);
                LOGGER.debug("pathTxt raw value: '{}'", params.get("pathTxt"));
                LOGGER.debug("pathTxt cleaned value: '{}'", cleanPath(params.get("pathTxt")));
                // Use typeAuthorSearch if typeAuthor is not provided (due to 5-parameter limit)
                String typeAuthorValue = params.get("typeAuthor");
                if (typeAuthorValue == null || typeAuthorValue.isEmpty()) {
                    typeAuthorValue = params.getOrDefault("typeAuthorSearch", "created");
                }
                LOGGER.debug("Using typeAuthor value: '{}'", typeAuthorValue);
                return new ReportByDateAndAuthor(
                        siteNode,
                        isCreated(typeAuthorValue) ? BaseReport.SearchActionType.CREATION : BaseReport.SearchActionType.UPDATE,
                        cleanPath(params.get("pathTxt")),
                        params.getOrDefault("typeSearch", "pages"),
                        true,
                        Boolean.parseBoolean(params.getOrDefault("searchByDate", "false")),
                        params.getOrDefault("typeDateSearch", "created"),
                        params.get("dateBegin"),
                        params.get("dateEnd"),
                        Boolean.parseBoolean(params.getOrDefault("searchAuthor", "false")),
                        params.get("searchUsername"),
                        params.getOrDefault("typeAuthorSearch", "created"),
                        sortCol,
                        order
                );
            case "21":
                return new ReportByUnstranslated(
                        siteNode,
                        params.get("selectLanguageBU"),
                        cleanPath(params.get("pathTxt")),
                        params.get("selectTypeSearch")
                );
            case "22":
                return new ReportWipContent(
                        siteNode,
                        cleanPath(params.get("pathTxt")),
                        isPages(params.get("typeSearch")) ? BaseReport.SearchContentType.PAGE : BaseReport.SearchContentType.CONTENT,
                        sortCol,
                        order
                );
            case "23":
                return new ReportDisplayLinks(
                        siteNode,
                        cleanPath(params.get("pathTxtOrigin")),
                        cleanPath(params.get("pathTxtDestination"))
                );
            case "24":
                return new ReportContentMarkedForDeletion(
                        siteNode,
                        cleanPath(params.get("pathTxt")),
                        isPages(params.get("typeSearch")) ? BaseReport.SearchContentType.PAGE : BaseReport.SearchContentType.CONTENT,
                        sortCol,
                        order
                );
            case "28":
                return new ReportUnreferencedAssets(
                        siteNode,
                        cleanPath(params.get("assetsRootPath"))
                );
            default:
                throw new ContentReportException("Invalid reportId: " + reportId);
        }
    }

    private static boolean isPages(String value) {
        return StringUtils.equalsIgnoreCase(value, "pages");
    }

    private static boolean isCreated(String value) {
        return StringUtils.equalsIgnoreCase(value, "created");
    }

    private static int parseInt(String value, int defaultValue) {
        if (StringUtils.isNumeric(value)) {
            return Integer.parseInt(value);
        }
        return defaultValue;
    }

    private static String cleanPath(String value) {
        return value != null ? value.replace("'", "") : "";
    }
}
