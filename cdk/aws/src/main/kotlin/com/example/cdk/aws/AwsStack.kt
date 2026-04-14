package com.example.cdk.aws

import com.hashicorp.cdktf.*
import com.hashicorp.cdktf.providers.aws.api_gateway_api_key.ApiGatewayApiKey
import com.hashicorp.cdktf.providers.aws.api_gateway_api_key.ApiGatewayApiKeyConfig
import com.hashicorp.cdktf.providers.aws.api_gateway_deployment.ApiGatewayDeployment
import com.hashicorp.cdktf.providers.aws.api_gateway_deployment.ApiGatewayDeploymentConfig
import com.hashicorp.cdktf.providers.aws.api_gateway_integration.ApiGatewayIntegration
import com.hashicorp.cdktf.providers.aws.api_gateway_integration.ApiGatewayIntegrationConfig
import com.hashicorp.cdktf.providers.aws.api_gateway_method.ApiGatewayMethod
import com.hashicorp.cdktf.providers.aws.api_gateway_method.ApiGatewayMethodConfig
import com.hashicorp.cdktf.providers.aws.api_gateway_resource.ApiGatewayResource
import com.hashicorp.cdktf.providers.aws.api_gateway_resource.ApiGatewayResourceConfig
import com.hashicorp.cdktf.providers.aws.api_gateway_rest_api.ApiGatewayRestApi
import com.hashicorp.cdktf.providers.aws.api_gateway_rest_api.ApiGatewayRestApiConfig
import com.hashicorp.cdktf.providers.aws.api_gateway_stage.ApiGatewayStage
import com.hashicorp.cdktf.providers.aws.api_gateway_stage.ApiGatewayStageConfig
import com.hashicorp.cdktf.providers.aws.api_gateway_usage_plan.ApiGatewayUsagePlan
import com.hashicorp.cdktf.providers.aws.api_gateway_usage_plan.ApiGatewayUsagePlanApiStages
import com.hashicorp.cdktf.providers.aws.api_gateway_usage_plan.ApiGatewayUsagePlanConfig
import com.hashicorp.cdktf.providers.aws.api_gateway_usage_plan_key.ApiGatewayUsagePlanKey
import com.hashicorp.cdktf.providers.aws.api_gateway_usage_plan_key.ApiGatewayUsagePlanKeyConfig
import com.hashicorp.cdktf.providers.aws.cloudwatch_event_bus.CloudwatchEventBus
import com.hashicorp.cdktf.providers.aws.cloudwatch_event_bus.CloudwatchEventBusConfig
import com.hashicorp.cdktf.providers.aws.iam_policy.IamPolicy
import com.hashicorp.cdktf.providers.aws.iam_policy.IamPolicyConfig
import com.hashicorp.cdktf.providers.aws.iam_role.IamRole
import com.hashicorp.cdktf.providers.aws.iam_role.IamRoleConfig
import com.hashicorp.cdktf.providers.aws.iam_role_policy.IamRolePolicy
import com.hashicorp.cdktf.providers.aws.iam_role_policy.IamRolePolicyConfig
import com.hashicorp.cdktf.providers.aws.lambda_function.LambdaFunction
import com.hashicorp.cdktf.providers.aws.lambda_function.LambdaFunctionConfig
import com.hashicorp.cdktf.providers.aws.lambda_function.LambdaFunctionEnvironment
import com.hashicorp.cdktf.providers.aws.lambda_permission.LambdaPermission
import com.hashicorp.cdktf.providers.aws.lambda_permission.LambdaPermissionConfig
import com.hashicorp.cdktf.providers.aws.provider.AwsProvider
import com.hashicorp.cdktf.providers.aws.provider.AwsProviderConfig
import com.hashicorp.cdktf.providers.aws.s3_bucket.S3Bucket
import com.hashicorp.cdktf.providers.aws.s3_bucket.S3BucketConfig
import com.hashicorp.cdktf.providers.random_provider.provider.RandomProvider
import com.hashicorp.cdktf.providers.random_provider.string_resource.StringResource
import software.constructs.Construct

class AwsStack(
    scope: Construct,
    id: String,
) : TerraformStack(scope, id) {

    init {
        val regionVar = TerraformVariable(
            this,
            "DEPLOY_TARGET_REGION",
            TerraformVariableConfig.builder()
                .type("string")
                .description("The AWS region")
                .build()
        )
        val region = regionVar.stringValue

        val accountVar = TerraformVariable(
            this,
            "DEPLOY_TARGET_ACCOUNT",
            TerraformVariableConfig.builder()
                .type("string")
                .description("The AWS account")
                .build()
        )
        val account = accountVar.stringValue

        val mockNestTokenVar = TerraformVariable(
            this,
            "MOCKNEST_API_TOKEN",
            TerraformVariableConfig.builder()
                .type("string")
                .description("MockNest API Token")
                .build()
        )
        val mockNestToken = mockNestTokenVar.stringValue

        AwsProvider(
            this,
            "Aws",
            AwsProviderConfig.builder()
                .region(region)
                .build()
        )

        S3Backend(
            this,
            S3BackendConfig.builder()
                .region("\${region}")
                .bucket("kotlin-lambda-terraform-state")
                .key("demo-terraform-cdk/terraform.tfstate")
                .encrypt(true)
                .build()
        )

        RandomProvider(this, "Random")

        val bucketSuffix = StringResource.Builder.create(this, "bucketSuffix")
            .length(8)
            .special(false)
            .upper(false)
            .numeric(false)
            .build()
            .result

        val eventBus = CloudwatchEventBus(
            this,
            "Demo-Pets-Events-Bus",
            CloudwatchEventBusConfig.builder()
                .name("demo-pets-events-bus")
                .build()
        )

        val s3Bucket = S3Bucket(
            this,
            "DemoMockNestMappingsBucket",
            S3BucketConfig.builder()
                .bucket("demo-mocknest-mappings-$bucketSuffix")
                .build()
        )

        val lambdaRole = IamRole(
            this,
            "Demo-Spring-Clean-Architecture-Fun-Role",
            IamRoleConfig.builder()
                .name("Demo-Spring-Clean-Architecture-Fun-Role")
                .assumeRolePolicy(
                    """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Action": "sts:AssumeRole",
                          "Principal": { "Service": "lambda.amazonaws.com" },
                          "Effect": "Allow"
                        }
                      ]
                    }
                    """.trimIndent()
                )
                .build()
        )

        val policy = IamPolicy(
            this,
            "Demo-Spring-Clean-Architecture-Fun-Policy",
            IamPolicyConfig.builder()
                .name("Demo-Spring-Clean-Architecture-Fun-Policy")
                .dependsOn(listOf(s3Bucket))
                .policy(
                    """
                    {
                      "Version": "2012-10-17",
                      "Statement": [
                        {
                          "Effect": "Allow",
                          "Action": [
                            "logs:CreateLogGroup",
                            "logs:CreateLogStream",
                            "logs:PutLogEvents"
                          ],
                          "Resource": "arn:aws:logs:*:*:*"
                        },
                        {
                          "Effect": "Allow",
                          "Action": [
                            "s3:PutObject",
                            "s3:GetObject",
                            "s3:ListBucket",
                            "s3:DeleteObject"
                          ],
                          "Resource": [
                            "${s3Bucket.arn}",
                            "${s3Bucket.arn}/*"
                          ]
                        },
                        {
                          "Effect": "Allow",
                          "Action": [
                            "ses:SendEmail",
                            "ses:SendRawEmail"
                          ],
                          "Resource": "*"
                        },
                        {
                          "Effect": "Allow",
                          "Action": [
                            "events:PutEvents"
                          ],
                          "Resource": "${eventBus.arn}"
                        }
                      ]
                    }
                    """.trimIndent()
                )
                .build()
        )

        IamRolePolicy(
            this,
            "Demo-Spring-Clean-Architecture-Fun-RolePolicy",
            IamRolePolicyConfig.builder()
                .name("Demo-Spring-Clean-Architecture-Fun-RolePolicy")
                .policy(policy.policy)
                .role(lambdaRole.name)
                .build()
        )

        val lambdaFunction = LambdaFunction(
            this,
            "Demo-Spring-Clean-Architecture-Fun",
            LambdaFunctionConfig.builder()
                .functionName("Demo-Spring-Clean-Architecture-Fun")
                .handler("org.springframework.cloud.function.adapter.aws.FunctionInvoker")
                .runtime("java21")
                .s3Bucket("lambda-deployment-clean-architecture-example")
                .s3Key("demo-aws-function.jar")
                .sourceCodeHash(
                    Fn.filebase64sha256("../../../../../build/dist/demo-aws-function.jar")
                )
                .role(lambdaRole.arn)
                .dependsOn(listOf(s3Bucket, lambdaRole))
                .memorySize(1024)
                .environment(
                    LambdaFunctionEnvironment.builder()
                        .variables(
                            mapOf(
                                "SPRING_CLOUD_FUNCTION_DEFINITION" to "router",
                                "MAIN_CLASS" to "com.example.clean.architecture.Application",
                                "AWS_S3_BUCKET_NAME" to s3Bucket.bucket,
                                "SPRING_PROFILES_ACTIVE" to "dev",
                                "JAVA_TOOL_OPTIONS" to "-XX:+TieredCompilation -XX:TieredStopAtLevel=1",
                                "BORED_API_URL" to "https://bored-api.appbrewery.com/filter?type=social",
                                "MOCKNEST_API_TOKEN" to mockNestToken,
                                "EVENTBRIDGE_BUS_NAME" to eventBus.name
                            )
                        )
                        .build()
                )
                .timeout(120)
                .build()
        )

        // --------------------------------------------------------------------
        // Existing API-key API
        // --------------------------------------------------------------------

        val api = ApiGatewayRestApi(
            this,
            "Demo-Spring-Clean-Architecture-API",
            ApiGatewayRestApiConfig.builder()
                .name("Demo-Spring-Clean-Architecture-API")
                .description("API for Spring Clean Architecture Example")
                .build()
        )

        val resource = ApiGatewayResource(
            this,
            "Demo-Spring-Clean-Architecture-Resource",
            ApiGatewayResourceConfig.builder()
                .restApiId(api.id)
                .parentId(api.rootResourceId)
                .pathPart("{proxy+}")
                .build()
        )

        val method = ApiGatewayMethod(
            this,
            "Demo-Spring-Clean-Architecture-Method",
            ApiGatewayMethodConfig.builder()
                .restApiId(api.id)
                .resourceId(resource.id)
                .httpMethod("ANY")
                .authorization("NONE")
                .apiKeyRequired(true)
                .build()
        )

        val integration = ApiGatewayIntegration(
            this,
            "Demo-Spring-Clean-Architecture-Integration",
            ApiGatewayIntegrationConfig.builder()
                .restApiId(api.id)
                .resourceId(resource.id)
                .httpMethod(method.httpMethod)
                .integrationHttpMethod("POST")
                .type("AWS_PROXY")
                .uri("arn:aws:apigateway:${region}:lambda:path/2015-03-31/functions/${lambdaFunction.arn}/invocations")
                .build()
        )

        val deployment = ApiGatewayDeployment(
            this,
            "Demo-Spring-Clean-Architecture-Deployment",
            ApiGatewayDeploymentConfig.builder()
                .restApiId(api.id)
                .dependsOn(listOf(integration))
                .build()
        )

        val stage = ApiGatewayStage(
            this,
            "Demo-Spring-Clean-Architecture-Stage",
            ApiGatewayStageConfig.builder()
                .restApiId(api.id)
                .deploymentId(deployment.id)
                .stageName("demo")
                .build()
        )

        val apiKey = ApiGatewayApiKey(
            this,
            "Demo-Spring-Clean-Architecture-ApiKey",
            ApiGatewayApiKeyConfig.builder()
                .name("Demo-Spring-Clean-Architecture-ApiKey")
                .description("API Key for Spring Clean Architecture Example")
                .enabled(true)
                .build()
        )

        val usagePlan = ApiGatewayUsagePlan(
            this,
            "Demo-Spring-Clean-Architecture-UsagePlan",
            ApiGatewayUsagePlanConfig.builder()
                .name("Demo-Spring-Clean-Architecture-UsagePlan")
                .description("Usage Plan for Spring Clean Architecture Example")
                .apiStages(
                    listOf(
                        ApiGatewayUsagePlanApiStages.builder()
                            .apiId(api.id)
                            .stage(stage.stageName)
                            .build()
                    )
                )
                .build()
        )

        ApiGatewayUsagePlanKey(
            this,
            "Demo-Spring-Clean-Architecture-UsagePlanKey",
            ApiGatewayUsagePlanKeyConfig.builder()
                .keyId(apiKey.id)
                .keyType("API_KEY")
                .usagePlanId(usagePlan.id)
                .build()
        )

        LambdaPermission(
            this,
            "Demo-Spring-Clean-Architecture-Permission",
            LambdaPermissionConfig.builder()
                .functionName(lambdaFunction.functionName)
                .action("lambda:InvokeFunction")
                .principal("apigateway.amazonaws.com")
                .sourceArn("arn:aws:execute-api:$region:$account:${api.id}/*/${method.httpMethod}/${resource.pathPart}")
                .build()
        )

        // --------------------------------------------------------------------
        // Separate IAM-only API for POST /pets-events
        // --------------------------------------------------------------------

        val iamApi = ApiGatewayRestApi(
            this,
            "Demo-Pets-Events-Iam-API",
            ApiGatewayRestApiConfig.builder()
                .name("Demo-Pets-Events-Iam-API")
                .description("IAM-only API for pets events")
                .build()
        )

        val iamPetsEventsResource = ApiGatewayResource(
            this,
            "Demo-Pets-Events-Iam-Resource",
            ApiGatewayResourceConfig.builder()
                .restApiId(iamApi.id)
                .parentId(iamApi.rootResourceId)
                .pathPart("pets-events")
                .build()
        )

        val iamPetsEventsMethod = ApiGatewayMethod(
            this,
            "Demo-Pets-Events-Iam-Method",
            ApiGatewayMethodConfig.builder()
                .restApiId(iamApi.id)
                .resourceId(iamPetsEventsResource.id)
                .httpMethod("POST")
                .authorization("AWS_IAM")
                .apiKeyRequired(false)
                .build()
        )

        val iamPetsEventsIntegration = ApiGatewayIntegration(
            this,
            "Demo-Pets-Events-Iam-Integration",
            ApiGatewayIntegrationConfig.builder()
                .restApiId(iamApi.id)
                .resourceId(iamPetsEventsResource.id)
                .httpMethod(iamPetsEventsMethod.httpMethod)
                .integrationHttpMethod("POST")
                .type("AWS_PROXY")
                .uri("arn:aws:apigateway:${region}:lambda:path/2015-03-31/functions/${lambdaFunction.arn}/invocations")
                .build()
        )

        val iamDeployment = ApiGatewayDeployment(
            this,
            "Demo-Pets-Events-Iam-Deployment",
            ApiGatewayDeploymentConfig.builder()
                .restApiId(iamApi.id)
                .dependsOn(listOf(iamPetsEventsIntegration))
                .build()
        )

        val iamStage = ApiGatewayStage(
            this,
            "Demo-Pets-Events-Iam-Stage",
            ApiGatewayStageConfig.builder()
                .restApiId(iamApi.id)
                .deploymentId(iamDeployment.id)
                .stageName("demo")
                .build()
        )

        LambdaPermission(
            this,
            "Demo-Pets-Events-Iam-Permission",
            LambdaPermissionConfig.builder()
                .functionName(lambdaFunction.functionName)
                .action("lambda:InvokeFunction")
                .principal("apigateway.amazonaws.com")
                .sourceArn("arn:aws:execute-api:$region:$account:${iamApi.id}/*/POST/pets-events")
                .build()
        )

        TerraformOutput(
            this,
            "pets-events-iam-endpoint-arn",
            TerraformOutputConfig.builder()
                .value("arn:aws:execute-api:$region:$account:${iamApi.id}/demo/POST/pets-events")
                .description("ARN for the IAM-only pets-events endpoint")
                .build()
        )

        TerraformOutput(
            this,
            "pets-events-iam-endpoint-url",
            TerraformOutputConfig.builder()
                .value("${iamStage.invokeUrl}/pets-events")
                .description("URL for the IAM-only pets-events endpoint")
                .build()
        )
    }
}