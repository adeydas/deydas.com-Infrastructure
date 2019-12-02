package com.deydas.infrastructure;

import com.google.common.collect.ImmutableList;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateProps;
import software.amazon.awscdk.services.certificatemanager.ValidationMethod;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;

import java.util.List;

public class BlogCdkStack extends Stack {

  private static final String RESOURCES_NAMING_PREFIX = "deydas.com-";

  private static final String WEBSITE_BUCKET_NAME = String.format("%sbucket", RESOURCES_NAMING_PREFIX);
  private static final String SSL_CERTIFICATE_ID = String.format("%scertificate", RESOURCES_NAMING_PREFIX);
  private static final String SSL_CERTIFICATE_DOMAIN_NAME = "deydas.com";
  private static final List<String> SSL_CERTIFICATE_SUB_DOMAINS = ImmutableList.of(String.format("*.%s", SSL_CERTIFICATE_DOMAIN_NAME));
  private static final String CLOUD_FRONT_DISTRIBUTION_ID = String.format("%scloudfront", RESOURCES_NAMING_PREFIX);
  private static final String CLOUD_FRONT_LOGGING_BUCKET_NAME = String.format("%scloudfront-logs", RESOURCES_NAMING_PREFIX);
  private static final String ROUTE_53_ID = String.format("%sroute53", RESOURCES_NAMING_PREFIX);
  private static final String ROUTE_53_ZONE_NAME = "deydas.com";
  private static final String ROUTE_53_DOMAIN_A_RECORD_ID = String.format("%sdomainarecord", RESOURCES_NAMING_PREFIX);
  private static final String ROUTE_53_DOMAIN_CNAME_RECORD_ID = String.format("%sdomainawwwrecord", RESOURCES_NAMING_PREFIX);

  public BlogCdkStack(final Construct scope, final String id, final StackProps stackProps) {
    super(scope, id, stackProps);

    final Bucket websiteBucket = createWebsiteBucket();
    final Certificate acmCertificate = createAcmCertificate();
    final CloudFrontWebDistribution cloudfrontDistribution = createCloudfrontDistribution(acmCertificate, websiteBucket);
    createRoute53HostedZone(cloudfrontDistribution);
  }

  private void createRoute53HostedZone(final CloudFrontWebDistribution cloudFrontWebDistribution) {
    final PublicHostedZoneProps publicHostedZoneProps = PublicHostedZoneProps.builder()
            .zoneName(ROUTE_53_ZONE_NAME)
            .build();

    final PublicHostedZone publicHostedZone = new PublicHostedZone(this, ROUTE_53_ID, publicHostedZoneProps);

    final RecordTarget recordTarget = RecordTarget.fromAlias(new CloudFrontTarget(cloudFrontWebDistribution));
    final ARecordProps aRecordProps = ARecordProps.builder()
            .recordName(SSL_CERTIFICATE_DOMAIN_NAME)
            .target(recordTarget)
            .zone(publicHostedZone)
            .build();
    new ARecord(this, ROUTE_53_DOMAIN_A_RECORD_ID, aRecordProps);

    final CnameRecordProps cnameRecordProps = CnameRecordProps.builder()
            .domainName(SSL_CERTIFICATE_DOMAIN_NAME)
            .recordName("www")
            .zone(publicHostedZone)
            .build();
    new CnameRecord(this, ROUTE_53_DOMAIN_CNAME_RECORD_ID, cnameRecordProps);
  }

  private CloudFrontWebDistribution createCloudfrontDistribution(final Certificate acmCertificate,
                                                                 final Bucket websiteBucket) {
    final ViewerCertificateOptions viewerCertificateOptions = ViewerCertificateOptions.builder()
            .aliases(ImmutableList.of(ROUTE_53_ZONE_NAME, String.format("www.%s", ROUTE_53_ZONE_NAME)))
            .build();
    final ViewerCertificate viewerCertificate = ViewerCertificate.fromAcmCertificate(acmCertificate, viewerCertificateOptions);

    final CustomOriginConfig customOriginConfig = CustomOriginConfig.builder()
            .httpPort(80)
            .httpsPort(443)
            .domainName("deydas.com-bucket.s3-website-us-east-1.amazonaws.com")
            .originKeepaliveTimeout(Duration.seconds(5))
            .originReadTimeout(Duration.seconds(30))
            .originProtocolPolicy(OriginProtocolPolicy.HTTP_ONLY)
            .build();

    final List<Behavior> behaviorList = ImmutableList.of(Behavior.builder().isDefaultBehavior(true).build());
    final SourceConfiguration sourceConfiguration = SourceConfiguration.builder()
            .customOriginSource(customOriginConfig)
            .behaviors(behaviorList)
            .build();

    final LoggingConfiguration loggingConfiguration = LoggingConfiguration.builder()
            .bucket(createCloudfrontLoggingBucket())
            .includeCookies(false)
            .build();

    final CloudFrontWebDistributionProps cloudFrontWebDistributionProps = CloudFrontWebDistributionProps.builder()
            .viewerCertificate(viewerCertificate)
            .originConfigs(ImmutableList.of(sourceConfiguration))
            .loggingConfig(loggingConfiguration)
            .priceClass(PriceClass.PRICE_CLASS_ALL)
            .defaultRootObject("index.html")
            .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
            .build();

    return new CloudFrontWebDistribution(this, CLOUD_FRONT_DISTRIBUTION_ID, cloudFrontWebDistributionProps);
  }

  private Certificate createAcmCertificate() {
    final CertificateProps certificateProps = CertificateProps.builder()
            .domainName(SSL_CERTIFICATE_DOMAIN_NAME)
            .subjectAlternativeNames(SSL_CERTIFICATE_SUB_DOMAINS)
            .validationMethod(ValidationMethod.EMAIL)
            .build();
    return new Certificate(this, SSL_CERTIFICATE_ID, certificateProps);
  }

  private Bucket createWebsiteBucket() {
    final BucketProps bucketProps = BucketProps.builder()
            .bucketName(WEBSITE_BUCKET_NAME)
            .build();
    final Bucket websiteS3Bucket = new Bucket(this, WEBSITE_BUCKET_NAME, bucketProps);
    websiteS3Bucket.grantPublicAccess("*");
    return websiteS3Bucket;
  }

  private Bucket createCloudfrontLoggingBucket() {
    final BucketProps bucketProps = BucketProps.builder()
            .bucketName(CLOUD_FRONT_LOGGING_BUCKET_NAME)
            .build();
    return new Bucket(this, CLOUD_FRONT_LOGGING_BUCKET_NAME, bucketProps);
  }

}
