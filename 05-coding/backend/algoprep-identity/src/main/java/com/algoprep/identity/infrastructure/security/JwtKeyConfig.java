package com.algoprep.identity.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * RS256 key material and the JWT encoder and decoder built from it (BD security section 1).
 *
 * <p>Keys come from PEM properties. When both are blank an ephemeral key pair is generated at
 * startup: fine for local development, but every restart invalidates outstanding access tokens, so
 * a real deployment must configure them.
 */
@Configuration
class JwtKeyConfig {

  private static final Log LOG = LogFactory.getLog(JwtKeyConfig.class);
  private static final int GENERATED_KEY_BITS = 2048;

  @Bean
  RSAKey jwtSigningKey(
      @Value("${algoprep.security.jwt.private-key:}") String privatePem,
      @Value("${algoprep.security.jwt.public-key:}") String publicPem) {
    try {
      if (privatePem.isBlank() && publicPem.isBlank()) {
        LOG.warn("No JWT key configured: generated an ephemeral RSA key, tokens die on restart");
        return generate();
      }
      if (privatePem.isBlank() || publicPem.isBlank()) {
        throw new IllegalStateException(
            "Set both algoprep.security.jwt.private-key and public-key, or neither");
      }
      return new RSAKey.Builder(publicKey(publicPem))
          .privateKey(privateKey(privatePem))
          .keyID(UUID.nameUUIDFromBytes(publicPem.getBytes()).toString())
          .build();
    } catch (GeneralSecurityExceptionWrapper
        | NoSuchAlgorithmException
        | InvalidKeySpecException e) {
      throw new IllegalStateException("Cannot load JWT signing key", e);
    }
  }

  @Bean
  JwtEncoder jwtEncoder(RSAKey key) {
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
  }

  @Bean
  JwtDecoder jwtDecoder(RSAKey key) throws java.security.spec.InvalidKeySpecException {
    try {
      NimbusJwtDecoder decoder =
          NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey())
              .signatureAlgorithm(SignatureAlgorithm.RS256)
              .build();
      OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefault();
      decoder.setJwtValidator(validator);
      return decoder;
    } catch (com.nimbusds.jose.JOSEException e) {
      throw new IllegalStateException("Cannot build JWT decoder", e);
    }
  }

  private static RSAKey generate() throws NoSuchAlgorithmException {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(GENERATED_KEY_BITS);
    KeyPair pair = generator.generateKeyPair();
    return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
        .privateKey((RSAPrivateKey) pair.getPrivate())
        .keyID(UUID.randomUUID().toString())
        .build();
  }

  private static RSAPublicKey publicKey(String pem)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] der = Base64.getMimeDecoder().decode(stripPem(pem));
    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
  }

  private static RSAPrivateKey privateKey(String pem)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] der = Base64.getMimeDecoder().decode(stripPem(pem));
    return (RSAPrivateKey)
        KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
  }

  private static String stripPem(String pem) {
    return pem.replaceAll("-----(BEGIN|END)[A-Z ]+-----", "").replaceAll("\s", "");
  }

  /** Placeholder so the catch above stays a single multi-catch; never thrown. */
  private static final class GeneralSecurityExceptionWrapper extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
