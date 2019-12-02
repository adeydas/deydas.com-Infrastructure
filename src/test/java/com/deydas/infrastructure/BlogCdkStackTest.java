package com.deydas.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringJoiner;

import static org.junit.Assert.*;

public class BlogCdkStackTest {
  private final static ObjectMapper JSON =
          new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true);

  @Test
  public void testStack() throws IOException {
    App app = new App();
    Environment envUSA = makeEnv("test-account", "us-east-1");
    BlogCdkStack stack = new BlogCdkStack(app, "test", StackProps.builder()
            .env(envUSA).build());

    File file = new File(getClass().getClassLoader().getResource("test-data.json").getFile());
    BufferedReader br = new BufferedReader(new FileReader(file));
    StringJoiner joiner = new StringJoiner("\n");
    String line = br.readLine();
    while (line != null) {
      joiner.add(line);
      line = br.readLine();
    }

    JsonNode actual = JSON.valueToTree(app.synth().getStackArtifact(stack.getArtifactId()).getTemplate());
    assertEquals(new ObjectMapper().readTree(joiner.toString()), actual);
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