/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.contentreports.bean;

import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * BaseReport Interface.
 * Created by Juan Carlos Rodas.
 */
public abstract class BaseReport {
    public static final boolean SORT_ASC = true;
    public static final  boolean SORT_DESC = false;

    /**
     *  the enum SEARCH_DATE_TYPE
     */
    public enum SEARCH_DATE_TYPE{ALL, BEFORE_DATE}

    /**
     * the enum SearchActionType
     */
    public enum SearchActionType {UPDATE, CREATION}

    /**
     * the enum SearchContentType
     */
    public enum SearchContentType {PAGE, CONTENT}

    protected JCRSiteNode siteNode;

    protected DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    protected Locale locale;
    protected Locale defaultLocale;
    protected Map<String, Locale> localeMap;

    protected BaseReport(JCRSiteNode siteNode) {
        this(siteNode, null);
    }

    protected BaseReport(JCRSiteNode siteNode, Locale requestedLocale) {
        this.siteNode = siteNode;
        this.localeMap = new HashMap<>();

        Locale siteDefault = null;
        for (Locale siteLocale : siteNode.getLanguagesAsLocales()) {
            this.localeMap.put(siteLocale.toString(), siteLocale);
            this.localeMap.putIfAbsent(siteLocale.getLanguage(), siteLocale);
            if (siteLocale.getLanguage().equalsIgnoreCase(siteNode.getDefaultLanguage())) {
                siteDefault = siteLocale;
            }
        }

        Locale resolvedLocale = requestedLocale != null ? requestedLocale : siteDefault;
        if (resolvedLocale == null && !localeMap.isEmpty()) {
            resolvedLocale = localeMap.values().iterator().next();
        }
        if (resolvedLocale == null) {
            resolvedLocale = Locale.ENGLISH;
        }

        this.locale = resolvedLocale;
        this.defaultLocale = siteDefault != null ? siteDefault : resolvedLocale;
    }

    /**
     * Builds the standard JSON response with recordsTotal, recordsFiltered, siteName,
     * siteDisplayableName and data keys. Subclass getJson() implementations call this
     * instead of repeating the five-line block.
     *
     * @param totalContent number of total/filtered records
     * @param data         the already-built data array
     * @return populated {@link JSONObject}
     * @throws JSONException
     */
    protected JSONObject buildJsonResponse(long totalContent, JSONArray data) throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("recordsTotal", totalContent);
        jsonObject.put("recordsFiltered", totalContent);
        jsonObject.put("siteName", siteNode.getName());
        jsonObject.put("siteDisplayableName", siteNode.getDisplayableName());
        jsonObject.put("data", data);
        return jsonObject;
    }

    public abstract void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException;

    public void setLocale(Locale locale) {
        if (locale != null) {
            this.locale = locale;
        }
    }

    /**
     * getJson
     * <p>get json to the iReport class,
     * custom implementation in each child class,
     * return a specific json for each report.</p>
     *
     * @return {@link JSONObject}
     * @throws JSONException
     * @throws RepositoryException
     */
    public abstract JSONObject getJson() throws JSONException, RepositoryException;

}
