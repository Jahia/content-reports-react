package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.admin.GqlAdminQuery;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

import javax.jcr.RepositoryException;

@GraphQLTypeExtension(GqlAdminQuery.class)
@GraphQLDescription("Content report GraphQL entry point for admin tooling")
public class ContentReportsQueryExtension {

    @GraphQLField
    @GraphQLDescription("Access content reports for a given site")
    public static GqlContentReports contentReports(@GraphQLName("siteKey") @GraphQLNonNull String siteKey,
                                                   @GraphQLName("language") @GraphQLDescription("Language/locale to use (e.g. en, en-US)") String language)
            throws RepositoryException {
        return new GqlContentReports(siteKey, language);
    }

    @GraphQLField
    @GraphQLName("contentReportsUsersGroups")
    @GraphQLDescription("Access users and groups report administration")
    // The report lists every user of the platform: server-level user administration only.
    // `admin` alone lets any editor of any site through (verified on 8.2.3.2).
    @GraphQLRequiresPermission("adminUsers")
    public static GqlContentReportsUsersGroups contentReportsUsersGroups() {
        return new GqlContentReportsUsersGroups();
    }
}
