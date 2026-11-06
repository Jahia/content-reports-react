package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.admin.GqlAdminMutation;
import org.jahia.modules.graphql.provider.dxm.security.GraphQLRequiresPermission;

@GraphQLTypeExtension(GqlAdminMutation.class)
@GraphQLDescription("Content report GraphQL mutation entry point for admin tooling")
public class ContentReportsMutationExtension {

    @GraphQLField
    @GraphQLName("contentReportsUsersGroups")
    @GraphQLDescription("Access users and groups report mutations")
    // The report lists the users of the platform: server-level user administration only
    @GraphQLRequiresPermission("adminUsers")
    public static GqlContentReportsUsersGroupsMutations contentReportsUsersGroups() {
        return new GqlContentReportsUsersGroupsMutations();
    }
}
