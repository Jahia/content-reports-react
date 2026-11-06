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
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONException;

import javax.jcr.RepositoryException;

/**
 * ReportByAuthorDetailed Class — filtered variant of {@link ReportByAuthor}.
 * <p>
 * Created by Juan Carlos Rodas.
 */
public class ReportByAuthorDetailed extends ReportByAuthor {

    private String username;

    /**
     * The ReportByAuthorDetailed constructor.
     *
     * @param reportType    {@link SearchContentType}
     * @param actionType    {@link SearchActionType}
     * @param useSystemUser {@link Boolean}
     */
    public ReportByAuthorDetailed(JCRSiteNode siteNode, String username, String path, SearchContentType reportType, SearchActionType actionType, Boolean useSystemUser) {
        super(siteNode, path, reportType, actionType, useSystemUser);
        this.username = username;
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
        String strQuery = "SELECT * FROM ";
        strQuery += (reportType.equals(SearchContentType.PAGE) ? "[jnt:page] " : "[jmix:editorialContent] ");
        strQuery += "AS item WHERE [" + propertyName + "]='" + JCRContentUtils.sqlEncode(username) + "' AND ISDESCENDANTNODE(item,['" + searchPath + "'])";
        fillReport(session, strQuery, offset, limit);
    }
}

