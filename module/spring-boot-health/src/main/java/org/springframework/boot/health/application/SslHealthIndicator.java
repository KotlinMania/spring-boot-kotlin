/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.kotlinmania.spring.boot.health.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.github.kotlinmania.spring.boot.health.contributor.AbstractHealthIndicator;
import io.github.kotlinmania.spring.boot.health.contributor.Health;
import io.github.kotlinmania.spring.boot.health.contributor.HealthIndicator;
import io.github.kotlinmania.spring.boot.health.contributor.Status;
import io.github.kotlinmania.spring.boot.info.SslInfo;
import io.github.kotlinmania.spring.boot.info.SslInfo.BundleInfo;
import io.github.kotlinmania.spring.boot.info.SslInfo.CertificateChainInfo;
import io.github.kotlinmania.spring.boot.info.SslInfo.CertificateInfo;
import io.github.kotlinmania.spring.boot.info.SslInfo.CertificateValidityInfo;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} that checks the certificates the application uses and reports
 * {@link Status#OUT_OF_SERVICE} when a certificate is invalid.
 *
 * @author Jonatan Ivanov
 * @author Young Jae You
 * @since 4.0.0
 */
public class SslHealthIndicator extends AbstractHealthIndicator {

	private final SslInfo sslInfo;

	private final Duration expiryThreshold;

	public SslHealthIndicator(SslInfo sslInfo, Duration expiryThreshold) {
		super("SSL health check failed");
		Assert.notNull(sslInfo, "'sslInfo' must not be null");
		this.sslInfo = sslInfo;
		this.expiryThreshold = expiryThreshold;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		List<CertificateChainInfo> validCertificateChains = new ArrayList<>();
		List<CertificateChainInfo> invalidCertificateChains = new ArrayList<>();
		List<CertificateChainInfo> expiringCertificateChains = new ArrayList<>();
		for (BundleInfo bundle : this.sslInfo.getBundles()) {
			for (CertificateChainInfo certificateChain : bundle.getCertificateChains()) {
				if (containsOnlyValidCertificates(certificateChain)) {
					validCertificateChains.add(certificateChain);
					if (containsExpiringCertificate(certificateChain)) {
						expiringCertificateChains.add(certificateChain);
					}
				}
				else if (containsInvalidCertificate(certificateChain)) {
					invalidCertificateChains.add(certificateChain);
				}
			}
		}
		builder.status((invalidCertificateChains.isEmpty()) ? Status.UP : Status.OUT_OF_SERVICE);
		builder.withDetail("expiringChains", expiringCertificateChains);
		builder.withDetail("invalidChains", invalidCertificateChains);
		builder.withDetail("validChains", validCertificateChains);
	}

	private boolean containsOnlyValidCertificates(CertificateChainInfo certificateChain) {
		return validatableCertificates(certificateChain).allMatch(this::isValidCertificate);
	}

	private boolean containsInvalidCertificate(CertificateChainInfo certificateChain) {
		return validatableCertificates(certificateChain).anyMatch(this::isNotValidCertificate);
	}

	private boolean containsExpiringCertificate(CertificateChainInfo certificateChain) {
		return validatableCertificates(certificateChain).anyMatch(this::isExpiringCertificate);
	}

	private Stream<CertificateInfo> validatableCertificates(CertificateChainInfo certificateChain) {
		return certificateChain.getCertificates().stream().filter((certificate) -> certificate.getValidity() != null);
	}

	private boolean isValidCertificate(CertificateInfo certificate) {
		CertificateValidityInfo validity = certificate.getValidity();
		Assert.state(validity != null, "'validity' must not be null");
		return validity.getStatus().isValid();
	}

	private boolean isNotValidCertificate(CertificateInfo certificate) {
		return !isValidCertificate(certificate);
	}

	private boolean isExpiringCertificate(CertificateInfo certificate) {
		Instant validityEnds = certificate.getValidityEnds();
		Assert.state(validityEnds != null, "'validityEnds' must not be null");
		return Instant.now().plus(this.expiryThreshold).isAfter(validityEnds);
	}

}
