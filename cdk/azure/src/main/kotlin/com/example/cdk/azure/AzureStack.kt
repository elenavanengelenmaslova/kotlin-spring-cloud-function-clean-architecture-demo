package com.example.cdk.azure

import com.hashicorp.cdktf.*
import com.hashicorp.cdktf.providers.azurerm.application_insights.ApplicationInsights
import com.hashicorp.cdktf.providers.azurerm.application_insights.ApplicationInsightsConfig
import com.hashicorp.cdktf.providers.azurerm.data_azurerm_resource_group.DataAzurermResourceGroup
import com.hashicorp.cdktf.providers.azurerm.data_azurerm_resource_group.DataAzurermResourceGroupConfig
import com.hashicorp.cdktf.providers.azurerm.linux_function_app.*
import com.hashicorp.cdktf.providers.azurerm.provider.AzurermProvider
import com.hashicorp.cdktf.providers.azurerm.provider.AzurermProviderConfig
import com.hashicorp.cdktf.providers.azurerm.provider.AzurermProviderFeatures
import com.hashicorp.cdktf.providers.azurerm.role_assignment.RoleAssignment
import com.hashicorp.cdktf.providers.azurerm.role_assignment.RoleAssignmentConfig
import com.hashicorp.cdktf.providers.azurerm.service_plan.ServicePlan
import com.hashicorp.cdktf.providers.azurerm.service_plan.ServicePlanConfig
import com.hashicorp.cdktf.providers.azurerm.storage_account.StorageAccount
import com.hashicorp.cdktf.providers.azurerm.storage_account.StorageAccountConfig
import com.hashicorp.cdktf.providers.azurerm.storage_container.StorageContainer
import com.hashicorp.cdktf.providers.azurerm.storage_container.StorageContainerConfig
import software.constructs.Construct


class AzureStack(scope: Construct, id: String) :
    TerraformStack(scope, id) {

    init {
        val resourceGroupName = "\${resource_group_name}"
        val functionAppName =
            "demo-spring-clean-architecture-fun"
        val appServicePlanName =
            "demo_serverless_app_plan"

        val storageAccountName = "\${storage_account_name}"

        // Configure the Azure Provider
        AzurermProvider(
            this,
            "Azure",
            AzurermProviderConfig.builder()
                .subscriptionId("\${subscription_id}")
                .clientId("\${client_id}")
                .clientSecret("\${client_secret}")
                .tenantId("\${tenant_id}")
                .features(
                    mutableListOf(
                        AzurermProviderFeatures.builder().build()
                    )
                )
                .build()
        )


        // Configure Terraform Backend to Use Azure Blob Storage
        AzurermBackend(
            this,
            AzurermBackendConfig.builder()
                .resourceGroupName("\${resource_group_name}")
                .storageAccountName("\${storage_account_name}")
                .containerName("vintikterraformstorage")
                .key("demo-kscfunction/terraform.tfstate")
                .clientId("\${client_id}")
                .clientSecret("\${client_secret}")
                .subscriptionId("\${subscription_id}")
                .tenantId("\${tenant_id}")
                .build()
        )
        // Reference the existing Resource Group
        val resourceGroup = DataAzurermResourceGroup(
            this,
            "ExistingResourceGroup",
            DataAzurermResourceGroupConfig.builder()
                .name(resourceGroupName) // Use existing resource group name
                .build()
        )

        // Create Storage Account for Blob Storage
        val storageAccount = StorageAccount(
            this,
            "demo-mocknest-sa",
            StorageAccountConfig.builder()
                .name("demomocknest")  // ✅ Storage account name
                .resourceGroupName(resourceGroup.name)
                .location(resourceGroup.location)
                .accountTier("Standard")
                .accountReplicationType("LRS")
                .build()
        )

        // Create a Blob Storage Container for MockNest Mappings
        val storageContainer = StorageContainer(
            this,
            "demo-mocknest-container",
            StorageContainerConfig.builder()
                .name("demo-mocknest-mappings")  // Blob storage container name
                .storageAccountName(storageAccount.name)
                .containerAccessType("private")  // Private access for security
                .dependsOn(listOf(storageAccount))
                .build()
        )

        // Create an App Service Plan
        val servicePlan = ServicePlan(
            this, "DemoSpringCloudExampleServicePlan",
            ServicePlanConfig.builder()
                .dependsOn(listOf(resourceGroup))
                .name(appServicePlanName)
                .resourceGroupName(resourceGroup.name)
                .osType("Linux")
                .skuName("Y1")
                .location(resourceGroup.location)
                .build()
        )

        // Create an Application Insights resource
        val appInsights = ApplicationInsights(
            this, "AppInsights",
            ApplicationInsightsConfig.builder()
                .name("demo-spring-cloud-app-insights")
                .resourceGroupName(resourceGroup.name)
                .location(resourceGroup.location)
                .applicationType("java")
                .build()
        )
        val storageAccountAccessKeyVar = TerraformVariable(
            this,
            "AZURE_STORAGE_ACCOUNT_ACCESS_KEY",
            TerraformVariableConfig.builder()
                .type("string")
                .description("The AWS region")
                .build()
        )

        val storageAccountAccessKey = storageAccountAccessKeyVar.stringValue

        // Create the Function App
        val functionApp = LinuxFunctionApp(
            this, "DemoSpringCloudFunctionApp",
            LinuxFunctionAppConfig.builder()
                .dependsOn(
                    listOf(
                        resourceGroup,
                        servicePlan,
                        appInsights,
                    )
                )
                .name(functionAppName)
                .resourceGroupName(resourceGroup.name)
                .location(resourceGroup.location)
                .servicePlanId(servicePlan.id)
                .storageAccountName(storageAccountName)
                .storageAccountAccessKey(
                    storageAccountAccessKey
                )
                .siteConfig(
                    LinuxFunctionAppSiteConfig.builder()
                        .applicationStack(
                            LinuxFunctionAppSiteConfigApplicationStack.builder()
                                .javaVersion("21")
                                .build()
                        ).build()
                )
                .identity(
                    LinuxFunctionAppIdentity.builder()
                        .type("SystemAssigned")
                        .build() // ✅ Enables Managed Identity
                )
                .appSettings(
                    mapOf(
                        "MAIN_CLASS" to "com.example.clean.architecture.Application",
                        "APPINSIGHTS_INSTRUMENTATIONKEY" to appInsights.instrumentationKey,
                        "WEBSITE_RUN_FROM_PACKAGE" to "1",
                    )
                )
                .build()
        )
        // Assign Function App Storage Permissions (Read/Write)
        val functionAppBlobStorageRole = RoleAssignment(
            this,
            "DemoFunctionAppBlobStorageRole",
            RoleAssignmentConfig.builder()
                .scope(storageAccount.id)  // Assign access at the Storage Account level
                .roleDefinitionName("Storage Blob Data Contributor")  // Allows reading and writing blobs
                .principalId(functionApp.identity.principalId)  // Assign to Function App's Managed Identity
                .build()
        )

        val functionAppStorageContributorRole = RoleAssignment(
            this,
            "DemoFunctionAppStorageContributorRole",
            RoleAssignmentConfig.builder()
                .scope(storageAccount.id) // ✅ Give access to full Storage Account management
                .roleDefinitionName("Storage Account Contributor") // ✅ Allows creating/deleting tables
                .principalId(functionApp.identity.principalId) // ✅ Assign to Function App's Managed Identity
                .build()
        )

    }
}
