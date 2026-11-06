package org.jahia.modules.contentreports.bean;

import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.utils.LanguageCodeConverters;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/**
 * Report 31: pages and content displayed in some of the site's languages only.
 *
 * Editors switch a node off per language in Content Editor (Visibility > Languages), which
 * stores the languages it is hidden in as {@code j:invalidLanguages}. Page Builder still draws
 * such a node in every language; preview and live leave it out. One row per page or content
 * hidden in at least one language of the site: title, path, type, the languages it is
 * displayed in and the languages it is hidden in. Languages no longer on the site are ignored,
 * and so are nodes carrying an empty value (Jahia keeps the property once it was set).
 *
 * Runs on the caller's session, so JCR ACLs apply. The list is returned whole and paginated on
 * the client like the other reports.
 */
public class ReportPartialLanguageVisibility extends BaseReport {

    private static final String INVALID_LANGUAGES = "j:invalidLanguages";
    private static final String SEPARATOR = ", ";

    private final String searchPath;
    private final List<JSONArray> rows = new ArrayList<>();

    /**
     * @param siteNode   the site to report on
     * @param searchPath the path under which nodes are searched (the site when empty)
     */
    public ReportPartialLanguageVisibility(JCRSiteNode siteNode, String searchPath) {
        super(siteNode);
        this.searchPath = searchPath == null || searchPath.isEmpty() ? siteNode.getPath() : searchPath;
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        rows.clear();
        Set<String> siteLanguages = new TreeSet<>(siteNode.getLanguages());

        String statement = "SELECT * FROM [jmix:i18n] AS node WHERE ISDESCENDANTNODE(node, ['" + searchPath + "'])"
                + " AND node.[" + INVALID_LANGUAGES + "] IS NOT NULL ORDER BY node.[jcr:path]";
        Query query = session.getWorkspace().getQueryManager().createQuery(statement, Query.JCR_SQL2);
        NodeIterator nodes = query.execute().getNodes();
        while (nodes.hasNext()) {
            JCRNodeWrapper node = (JCRNodeWrapper) nodes.nextNode();
            if (node.isNodeType("jnt:page") || node.isNodeType("jnt:content")) {
                addRow(node, siteLanguages);
            }
        }
    }

    private void addRow(JCRNodeWrapper node, Set<String> siteLanguages) throws RepositoryException {
        Set<String> hidden = new TreeSet<>();
        for (Value value : node.getProperty(INVALID_LANGUAGES).getValues()) {
            if (siteLanguages.contains(value.getString())) {
                hidden.add(value.getString());
            }
        }
        if (hidden.isEmpty()) {
            return;
        }
        Set<String> displayed = new TreeSet<>(siteLanguages);
        displayed.removeAll(hidden);

        JSONArray row = new JSONArray();
        row.put(titleOf(node));
        row.put(node.getPath());
        row.put(node.getPrimaryNodeType().getLabel(locale));
        row.put(String.join(SEPARATOR, displayed));
        row.put(String.join(SEPARATOR, hidden));
        rows.add(row);
    }

    /**
     * The node's {@code jcr:title} in the site language matching the report language
     * ({@code en} finds {@code en_HK}), else in the site's default language, else in any site
     * language (a page hidden in the default language often has no title there), else its name.
     * The report session has no locale, so {@code getDisplayableName()} would always answer
     * the name.
     */
    private String titleOf(JCRNodeWrapper node) throws RepositoryException {
        Locale siteLocale = localeMap.containsKey(locale.toString()) ? localeMap.get(locale.toString()) : localeMap.get(locale.getLanguage());
        Locale siteDefault = LanguageCodeConverters.languageCodeToLocale(siteNode.getDefaultLanguage());
        List<Locale> candidates = new ArrayList<>();
        candidates.add(siteLocale);
        candidates.add(siteDefault);
        candidates.addAll(siteNode.getLanguagesAsLocales());
        for (Locale titleLocale : candidates) {
            if (titleLocale != null && node.hasI18N(titleLocale)) {
                Node translation = node.getI18N(titleLocale);
                if (translation.hasProperty(Constants.JCR_TITLE)) {
                    String title = translation.getProperty(Constants.JCR_TITLE).getString();
                    if (!title.isEmpty()) {
                        return title;
                    }
                }
            }
        }
        return node.getName();
    }

    @Override
    public JSONObject getJson() throws JSONException, RepositoryException {
        JSONArray data = new JSONArray();
        for (JSONArray row : rows) {
            data.put(row);
        }

        JSONObject json = new JSONObject();
        json.put("recordsTotal", rows.size());
        json.put("recordsFiltered", rows.size());
        json.put("siteName", siteNode.getName());
        json.put("siteDisplayableName", siteNode.getDisplayableName());
        json.put("data", data);
        return json;
    }
}
