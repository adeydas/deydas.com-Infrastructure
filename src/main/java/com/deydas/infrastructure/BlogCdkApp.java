package com.deydas.infrastructure;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

public class BlogCdkApp {

  private static final String ACCOUNT_ID = "977481931293";
  private static final String DEPLOYMENT_REGION = "us-east-1";

  public static void main(final String argv[]) {
    App app = new App();

    Environment envUSA = makeEnv(ACCOUNT_ID, DEPLOYMENT_REGION);

    new BlogCdkStack(app, "BlogCdkStack", StackProps.builder()
            .env(envUSA).build());

    app.synth();
  }

  private static Environment makeEnv(String account, String region) {
    account = (account == null) ? System.getenv("CDK_DEFAULT_ACCOUNT") : account;
    region = (region == null) ? System.getenv("CDK_DEFAULT_REGION") : region;

    return Environment.builder()
            .account(account)
            .region(region)
            .build();
  }

}
