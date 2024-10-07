package com.iqkv.sample.webmvc.dashboard.web.rest;

import static com.iqkv.sample.webmvc.dashboard.security.SecurityUtils.AUTHORITIES_KEY;
import static com.iqkv.sample.webmvc.dashboard.security.SecurityUtils.JWT_ALGORITHM;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iqkv.boot.security.SecurityProperties;
import com.iqkv.sample.webmvc.dashboard.web.rest.vm.LoginVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
public class AuthenticateController {

  private static final Logger LOG = LoggerFactory.getLogger(AuthenticateController.class);

  private final SecurityProperties securityProperties;
  private final JwtEncoder jwtEncoder;
  private final AuthenticationManagerBuilder authenticationManagerBuilder;

  public AuthenticateController(SecurityProperties securityProperties, JwtEncoder jwtEncoder, AuthenticationManagerBuilder authenticationManagerBuilder) {
    this.securityProperties = securityProperties;
    this.jwtEncoder = jwtEncoder;
    this.authenticationManagerBuilder = authenticationManagerBuilder;
  }

  @PostMapping("/authenticate")
  public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {
    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
        loginVM.getUsername(),
        loginVM.getPassword()
    );

    Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = this.createToken(authentication, loginVM.isRememberMe());
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setBearerAuth(jwt);
    return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
  }

  /**
   * {@code GET /authenticate} : check if the user is authenticated, and return its login.
   *
   * @param request the HTTP request.
   * @return the login if the user is authenticated.
   */
  @GetMapping("/authenticate")
  public String isAuthenticated(HttpServletRequest request) {
    LOG.debug("REST request to check if the current user is authenticated");
    return request.getRemoteUser();
  }

  public String createToken(Authentication authentication, boolean rememberMe) {
    String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

    Instant now = Instant.now();
    Instant validity;
    if (rememberMe) {
      validity = now.plus(this.securityProperties.getAuthentication().getJwt().getTokenValidityInSecondsForRememberMe(), ChronoUnit.SECONDS);
    } else {
      validity = now.plus(this.securityProperties.getAuthentication().getJwt().getTokenValidityInSeconds(), ChronoUnit.SECONDS);
    }

    // @formatter:off
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuedAt(now)
        .expiresAt(validity)
        .subject(authentication.getName())
        .claim(AUTHORITIES_KEY, authorities)
        .build();

    JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();
    return this.jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
  }

  /**
   * Object to return as body in JWT Authentication.
   */
  static class JWTToken {

    private String idToken;

    JWTToken(String idToken) {
      this.idToken = idToken;
    }

    @JsonProperty("id_token")
    String getIdToken() {
      return idToken;
    }

    void setIdToken(String idToken) {
      this.idToken = idToken;
    }
  }
}