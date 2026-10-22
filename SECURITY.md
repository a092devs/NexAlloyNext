# Security policy

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability or an accidentally
published credential. Report it privately through GitHub's security
advisory workflow, or contact the repository maintainers through the contact
method configured for the repository.

If a credential may have been exposed, revoke or rotate it first where
possible, then include only the minimum evidence needed to investigate.

## Credentials and client keys

Do not commit passwords, signing keys, access tokens, private keys, or local
machine configuration. The Google API keys used by some upstream Android
client flows are client-side keys, not server credentials; they must still be
restricted to the intended APIs and quotas in Google Cloud. Do not copy this
pattern for secrets that grant privileged backend access.

## Android release signing

Stable releases must use the protected release keystore configured in GitHub
Actions. Keep `RELEASE_CERT_SHA256` configured with the certificate fingerprint
without separators; the release workflow verifies it before building. Never
replace the release keystore without a documented Android signing-key migration
plan, because existing installations must be able to verify updates.
