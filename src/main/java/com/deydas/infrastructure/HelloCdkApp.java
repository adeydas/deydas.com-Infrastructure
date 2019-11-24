package com.deydas.infrastructure;

import software.amazon.awscdk.core.App;

public class HelloCdkApp {
    public static void main(final String argv[]) {
        App app = new App();

        new HelloCdkStack(app, "HelloCdkStack");

        app.synth();
    }
}
