{%
  author: ${vlovgr}
  date: "2026-11-14"
  tags: [technical]
%}

# Jots: Functional JWTs for Scala

I am happy to announce that my library [Jots](https://vlovgr.github.io/jots) has been accepted as a Typelevel affiliate project.

![Jots: Functional JWTs for Scala](/img/media/jots.png)

Jots is a library for working with [JSON Web Tokens (JWTs)](https://en.wikipedia.org/wiki/JSON_Web_Token) in Scala, Scala.js and Scala Native.

- Supports token signing and verification, including with [JSON Web Keys (JWKs)](https://vlovgr.github.io/jots/features/signing.html#json-web-key) and [JWK Sets](https://vlovgr.github.io/jots/features/verification.html#json-web-key-set).
- Integrates with [http4s](https://vlovgr.github.io/jots/integrations/http4s.html) for `AuthMiddleware` verification and periodically refreshing JWK Sets.
- Uses the Cats Effect, Circe, Literally and scodec-bits Typelevel libraries in the core module.

## Cryptography

Token signing and verification are built on an independent [`jots-crypto`](https://vlovgr.github.io/jots/features/cryptography.html) module.

- Provides a `Crypto[F]` capability for effect types with a `Sync` instance, like `IO` and `SyncIO`.
- Supports HMAC, plus signing and verification using ECDSA, EdDSA and RSA algorithms.
- Uses Java cryptography on the JVM, Node.js on Scala.js and OpenSSL on Scala Native.
- Includes a `String` interpolator for public keys, which are validated at compile time.

The module can be used on its own when you need cross-platform cryptography.

## Testing

The [`jots-testing`](https://vlovgr.github.io/jots/features/testing.html) module helps with writing tests for code using the library.

- Provides ScalaCheck generators and `Arbitrary` instances for tokens, keys, JWKs and more.
- Adds `String` interpolators for private keys, secret keys and tokens, validated at compile time.
- Allows tests to bypass signature verification, which is something the core module prevents.

## Getting Started

For more information and how to get started, head over to the [project website](https://vlovgr.github.io/jots).
