package me.ttajiri.testingawscdk;

import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.services.cloudfront.model.ListPublicKeysRequest;
import com.amazonaws.services.cloudfront.model.ListPublicKeysResult;
import com.amazonaws.services.cloudfront.model.PublicKeySummary;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;

public class TestingAwsCdkApplication {

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
				System.out.println(getCloudFrontSignedUrl());
	}

	private static String getCloudFrontSignedUrl() throws NoSuchAlgorithmException, InvalidKeySpecException {
		// CloudFront config
		String distributionDomain = "d1wel2wvrls1qh.cloudfront.net";
		String publicKeyPrefix = "invoice";
		String objectkey = "index.html";
		String policyResourcePath = "https://" + distributionDomain + "/" + objectkey;

		// SecretsManager config
		String secretsManagerId = "cloudfront-secret";


		AmazonCloudFront cloudFront = AmazonCloudFrontClientBuilder.defaultClient();
		ListPublicKeysRequest publicKeysRequest = new ListPublicKeysRequest();

		ListPublicKeysResult publicKeys = cloudFront.listPublicKeys(publicKeysRequest);
		PublicKeySummary summary = publicKeys.getPublicKeyList()
											 .getItems()
											 .stream()
											   .filter(s -> s.getName().contains(publicKeyPrefix))
											   .sorted(Comparator.comparing(PublicKeySummary::getCreatedTime).reversed())  // 最新のキーペアを取得
											   .findFirst()
											     .get();

		AWSSecretsManager secretsManager = AWSSecretsManagerClientBuilder.defaultClient();
		GetSecretValueRequest secretValueRequest = new GetSecretValueRequest();
		secretValueRequest.setSecretId(secretsManagerId);

		GetSecretValueResult secret = secretsManager.getSecretValue(secretValueRequest);

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(secret.getSecretBinary().array());
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey key = keyFactory.generatePrivate(keySpec);

		LocalDateTime date = LocalDateTime.now();
		date = date.plusSeconds(600);

		return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(policyResourcePath,
				  										        summary.getId(),
																key,
																Date.from(date.atZone(ZoneId.systemDefault()).toInstant()));
	}
}
